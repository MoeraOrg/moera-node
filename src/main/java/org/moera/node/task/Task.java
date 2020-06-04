package org.moera.node.task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.UUID;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.event.Event;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.util.UriUtil;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public abstract class Task implements Runnable {

    protected static final String GET = "GET";
    protected static final String POST = "POST";
    protected static final String PUT = "PUT";
    protected static final String DELETE = "DELETE";

    private static final Duration CALL_API_CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CALL_API_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    protected UUID nodeId;
    protected String nodeName;
    protected PrivateKey signingKey;

    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    private Domains domains;

    @Inject
    private NamingCache namingCache;

    @Inject
    private NamingClient namingClient;

    @Inject
    private EventManager eventManager;

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setSigningKey(PrivateKey signingKey) {
        this.signingKey = signingKey;
    }

    protected void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(nodeId));
    }

    protected String fetchNodeUri(String remoteNodeName) {
        namingCache.setNodeId(nodeId);
        RegisteredNameDetails details = namingCache.get(remoteNodeName);
        return details != null ? UriUtil.normalize(details.getNodeUri()) : null;
    }

    protected byte[] fetchSigningKey(String remoteNodeName, long at) {
        String namingLocation = domains.getDomainOptions(nodeId).getString("naming.location");
        RegisteredName registeredName = (RegisteredName) NodeName.parse(remoteNodeName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected void send(Event event) {
        eventManager.send(nodeId, event);
    }

    protected <T> T callApi(String method, String remoteNodeName, String location, Class<T> result)
            throws CallApiException {

        return callApi(method, remoteNodeName, location, null, result);
    }

    protected <T> T callApi(String method, String remoteNodeName, String location, Object body, Class<T> result)
            throws CallApiException {

        String nodeUri = fetchNodeUri(remoteNodeName);
        if (nodeUri == null) {
            throw new CallApiUnknownNameException(remoteNodeName);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUri + "/api" + location))
                .timeout(CALL_API_REQUEST_TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method(method, jsonPublisher(body))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CALL_API_CONNECTION_TIMEOUT)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new CallApiException(e);
        }
        switch (HttpStatus.valueOf(response.statusCode())) {
            case NOT_FOUND:
                throw new CallApiNotFoundException(response.uri());

            case OK:
            case CREATED:
                // do nothing
                break;

            default:
                throw new CallApiErrorStatusException(response.statusCode(), response.body());
        }
        return jsonParse(response.body(), result);
    }

    private HttpRequest.BodyPublisher jsonPublisher(Object body) throws CallApiException {
        if (body == null) {
            return HttpRequest.BodyPublishers.ofString("{}");
        }
        try {
            return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new CallApiException("Error converting to JSON", e);
        }
    }

    private <T> T jsonParse(String data, Class<T> klass) throws CallApiException {
        try {
            return objectMapper.readValue(data, klass);
        } catch (IOException e) {
            throw new BodyMappingException(e);
        }
    }

}
