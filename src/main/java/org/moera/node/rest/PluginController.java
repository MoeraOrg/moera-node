package org.moera.node.rest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthCategory;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.ProviderApi;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PluginDescription;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.plugin.DuplicatePluginException;
import org.moera.node.plugin.PluginDescriptor;
import org.moera.node.plugin.PluginInvocationException;
import org.moera.node.plugin.Plugins;
import org.moera.node.sse.StreamEmitter;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@ApiController
@RequestMapping("/moera/api/plugins")
@NoCache
public class PluginController {

    private static final Logger log = LoggerFactory.getLogger(PluginController.class);

    private static final Pattern PLUGIN_PATH = Pattern.compile("^.*/api/plugins/[^/]+/(.*)$");
    private static final Duration PLUGIN_CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration PLUGIN_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Plugins plugins;

    @Inject
    private TaskAutowire taskAutowire;

    @ProviderApi
    @PostMapping
    public Result post(@RequestBody @Valid PluginDescription pluginDescription) {
        log.info("POST /plugins (name = {})", LogUtil.format(pluginDescription.getName()));

        UUID nodeId;
        if (requestContext.isRootAdmin()) {
            nodeId = null;
        } else if (requestContext.isAdmin()) {
            nodeId = requestContext.nodeId();
        } else {
            throw new AuthenticationException();
        }

        PluginDescriptor descriptor = new PluginDescriptor(nodeId);
        pluginDescription.toDescriptor(descriptor);
        try {
            plugins.add(descriptor);
        } catch (DuplicatePluginException e) {
            throw new ValidationFailure("plugin.already-exists");
        }

        return Result.OK;
    }

    @ProviderApi
    @RequestMapping("/{pluginName}/**")
    public ResponseEntity<String> proxy(@PathVariable String pluginName, @RequestBody(required = false) String body,
                                        HttpMethod method, HttpServletRequest request)
            throws PluginInvocationException {

        log.info("{} /plugins/{pluginName}/... (pluginName = {})", method.name(), LogUtil.format(pluginName));

        PluginDescriptor descriptor = getPluginDescriptor(pluginName);
        if (ObjectUtils.isEmpty(descriptor.getLocation())) {
            throw new ObjectNotFoundFailure("not-supported");
        }

        try {
            HttpResponse<String> response = getPluginClient().send(getPluginRequest(descriptor, body, method, request),
                    HttpResponse.BodyHandlers.ofString());
            return ResponseEntity
                    .status(response.statusCode())
                    .headers(convertHeaders(response.headers()))
                    .body(response.body());
        } catch (HttpClientErrorException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        } catch (IOException | InterruptedException e) {
            throw new PluginInvocationException(e);
        }
    }

    private HttpClient getPluginClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(PLUGIN_CONNECTION_TIMEOUT)
                .build();
    }

    private URI getPluginUri(PluginDescriptor descriptor, HttpServletRequest request) {
        UriComponentsBuilder uriBuilder = UriUtil.createBuilderFromRequest(request);
        UriComponents location = UriComponentsBuilder.fromHttpUrl(descriptor.getLocation()).build();
        uriBuilder.scheme(location.getScheme());
        uriBuilder.host(location.getHost());
        uriBuilder.port(location.getPort());
        String path = uriBuilder.build().getPath();
        if (path == null) {
            throw new ObjectNotFoundFailure("not-found");
        }
        Matcher matcher = PLUGIN_PATH.matcher(path);
        if (!matcher.matches()) {
            throw new ObjectNotFoundFailure("not-found");
        }
        uriBuilder.replacePath(location.getPath());
        uriBuilder.path(matcher.group(1));
        return uriBuilder.build().toUri();
    }

    private HttpRequest getPluginRequest(PluginDescriptor descriptor, String body, HttpMethod method,
                                         HttpServletRequest request) {
        var bodyPublisher = body != null
                ? HttpRequest.BodyPublishers.ofString(body)
                : HttpRequest.BodyPublishers.noBody();
        var requestBuilder = HttpRequest.newBuilder()
                .uri(getPluginUri(descriptor, request))
                .timeout(PLUGIN_REQUEST_TIMEOUT)
                .method(method.name(), bodyPublisher);
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            if (!name.equalsIgnoreCase(HttpHeaders.HOST) && !name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                request.getHeaders(name).asIterator().forEachRemaining(value ->
                        requestBuilder.header(name, value));
            }
        });
        addContextHeaders(request, requestBuilder);
        return requestBuilder.build();
    }

    private void addContextHeaders(HttpServletRequest request, HttpRequest.Builder requestBuilder) {
        var vars = Map.<String, Object>of(
                "root-admin", requestContext.isRootAdmin(),
                "admin", requestContext.isAdmin(),
                "auth-category", String.join(",", AuthCategory.toStrings(requestContext.getAuthCategory())),
                "client-name", Optional.ofNullable(requestContext.getClientName()).orElse(""),
                "remote-address", getRemoteAddress(request),
                "user-agent", requestContext.getUserAgent().name().toLowerCase(),
                "user-agent-os", requestContext.getUserAgentOs().name().toLowerCase(),
                "node-id", requestContext.nodeId()
        );
        String headerValue = vars.entrySet().stream()
                .map(v -> v.getKey() + "=" + v.getValue())
                .collect(Collectors.joining(";"));
        requestBuilder.header("X-Moera-Auth", headerValue);
        requestBuilder.header("X-Moera-Origin", requestContext.getUrl());
        requestBuilder.header("X-Moera-Root", requestContext.getSiteUrl());
    }

    private String getRemoteAddress(HttpServletRequest request) {
        try {
            InetAddress address = UriUtil.remoteAddress(request);
            return address != null ? address.getHostAddress() : "";
        } catch (UnknownHostException e) {
            return "";
        }
    }

    private HttpHeaders convertHeaders(java.net.http.HttpHeaders headers) {
        HttpHeaders responseHeaders = new HttpHeaders();
        headers.map().forEach(responseHeaders::addAll);
        return responseHeaders;
    }

    @ProviderApi
    @GetMapping(value = "/{pluginName}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamEmitter get(@PathVariable String pluginName,
                             @RequestParam(name = "after", required = false) Long after,
                             @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId)
            throws Throwable {

        log.info("GET /plugins/{pluginName}/events (pluginName = {}, after = {}, Last-Event-ID = {})",
                LogUtil.format(pluginName), LogUtil.format(after), LogUtil.format(lastEventId));

        long lastSeenMoment = after != null ? after : (lastEventId != null ? lastEventId : 0);

        PluginDescriptor descriptor = getPluginDescriptor(pluginName);

        StreamEmitter emitter = new StreamEmitter();
        emitter.send(StreamEmitter.event().comment("ברוך הבא")); // To send HTTP headers immediately

        descriptor.replaceEventsSender(emitter, taskAutowire, lastSeenMoment);

        return emitter;
    }

    @ProviderApi
    @DeleteMapping("/{pluginName}")
    public Result delete(@PathVariable String pluginName) {
        log.info("DELETE /plugins/{pluginName} (pluginName = {})", LogUtil.format(pluginName));

        PluginDescriptor descriptor = getPluginDescriptor(pluginName);
        plugins.remove(descriptor);

        return Result.OK;
    }

    private PluginDescriptor getPluginDescriptor(String pluginName) {
        PluginDescriptor descriptor = null;
        if (requestContext.nodeId() != null) {
            descriptor = plugins.get(requestContext.nodeId(), pluginName);
        }
        if (descriptor == null) {
            descriptor = plugins.get(null, pluginName);
        }
        if (descriptor == null) {
            throw new ObjectNotFoundFailure("plugin.unknown");
        }
        if (descriptor.getNodeId() == null && !requestContext.isRootAdmin()) {
            throw new AuthenticationException();
        }
        if (!requestContext.isAdmin()) {
            throw new AuthenticationException();
        }
        return descriptor;
    }

}
