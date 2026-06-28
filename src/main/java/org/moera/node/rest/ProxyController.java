package org.moera.node.rest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import jakarta.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.moera.lib.node.types.LinkPreviewInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.config.Config;
import org.moera.node.global.ApiController;
import org.moera.node.linkpreviewnet.LinkPreviewNet;
import org.moera.node.linkpreviewnet.LinkPreviewNetException;
import org.moera.node.linkpreviewnet.LinkPreviewNetInfo;
import org.moera.node.media.MimeUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/proxy")
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(1);

    @Inject
    private Config config;

    @Inject
    private LinkPreviewNet linkPreviewNet;

    @GetMapping("/media")
    @Admin(Scope.OTHER)
    public ResponseEntity<Resource> getMedia(@RequestParam String url) {
        log.info("GET /proxy/media, (url = {})", LogUtil.format(url));

        OkHttpClient client = buildClient();
        Request request;
        try {
            request = new Request.Builder()
                .header(HttpHeaders.USER_AGENT, config.getUserAgent())
                .get()
                .url(url)
                .build();
        } catch (IllegalArgumentException e) {
            throw new ValidationFailure("proxy.url.invalid");
        }
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new OperationFailure("proxy.request-failed");
        }

        int statusCode = response.code();
        if (statusCode != HttpStatus.OK.value()) {
            response.close();
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                throw new ObjectNotFoundFailure("proxy.resource-not-found");
            } else {
                throw new OperationFailure("proxy.error-status");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        String contentType = response.header(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !MimeUtil.isSupportedImage(contentType)) {
            response.close();
            throw new ValidationFailure("proxy.resource-not-media");
        }
        headers.setContentType(MediaType.valueOf(contentType));
        ResponseBody body = response.body();
        if (body == null) {
            response.close();
            throw new OperationFailure("proxy.request-failed");
        }
        long contentLength = body.contentLength();
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }

        return new ResponseEntity<>(new InputStreamResource(body.byteStream()), headers, HttpStatus.OK);
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
            .protocols(List.of(Protocol.HTTP_1_1))
            .followRedirects(true)
            .connectTimeout(CONNECTION_TIMEOUT)
            .callTimeout(REQUEST_TIMEOUT)
            .build();
    }

    @GetMapping("/link-preview")
    @Admin(Scope.OTHER)
    public LinkPreviewInfo getLinkPreview(@RequestParam String url) {
        log.info("GET /proxy/link-preview, (url = {})", LogUtil.format(url));

        String host = URI.create(url).getHost();
        if (host == null) {
            throw new ObjectNotFoundFailure("proxy.resource-not-found");
        }

        for (String domain : config.getLinkPreview().getDomains()) {
            if (!host.matches(domain)) {
                continue;
            }
            return switch (config.getLinkPreview().getService()) {
                case "linkpreviewnet" -> queryLinkPreviewNet(url);
                default -> {
                    log.error("Unknown link preview service: " + LogUtil.format(config.getLinkPreview().getService()));
                    throw new OperationFailure("server.misconfiguration");
                }
            };
        }

        return queryDirectly(url);
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
            .header("User-Agent", config.getUserAgent("link preview"))
            .followRedirects(true)
            .timeout((int) REQUEST_TIMEOUT.toMillis())
            .get();
    }

    private LinkPreviewInfo queryDirectly(String url) {
        Document document;
        try {
            document = fetchDocument(url);
        } catch (IOException e) {
            throw new ObjectNotFoundFailure("proxy.resource-not-found");
        }

        LinkPreviewInfo linkPreviewInfo = new LinkPreviewInfo();
        linkPreviewInfo.setUrl(url);

        Elements elements = document.select("meta");
        for (Element element : elements) {
            String property = element.attr("property");
            if (ObjectUtils.isEmpty(property)) {
                property = element.attr("name");
            }
            analyzeMetaTag(linkPreviewInfo, property, element.attr("content"));
        }
        if (ObjectUtils.isEmpty(linkPreviewInfo.getTitle())) {
            Elements titles = document.select("head title");
            for (Element title : titles) {
                linkPreviewInfo.setTitle(title.ownText());
            }
        }
        return linkPreviewInfo;
    }

    private static void analyzeMetaTag(LinkPreviewInfo linkPreviewInfo, String property, String content) {
        switch (property) {
            case "og:site_name":
                linkPreviewInfo.setSiteName(content);
                break;
            case "og:url":
                linkPreviewInfo.setUrl(content);
                break;
            case "og:title":
                linkPreviewInfo.setTitle(content);
                break;
            case "og:description":
                linkPreviewInfo.setDescription(content);
                break;
            case "og:image":
                linkPreviewInfo.setImageUrl(content);
                break;
            case "article:published_time":
                Long publishedAt = parseTimestamp(content);
                if (publishedAt != null) {
                    linkPreviewInfo.setPublishedAt(publishedAt);
                }
                break;
            case "twitter:title":
            case "title":
                if (ObjectUtils.isEmpty(linkPreviewInfo.getTitle())) {
                    linkPreviewInfo.setTitle(content);
                }
                break;
            case "twitter:description":
            case "description":
                if (ObjectUtils.isEmpty(linkPreviewInfo.getDescription())) {
                    linkPreviewInfo.setDescription(content);
                }
                break;
            case "twitter:image":
                if (ObjectUtils.isEmpty(linkPreviewInfo.getImageUrl())) {
                    linkPreviewInfo.setImageUrl(content);
                }
                break;
            default:
                // ignore
        }
    }

    static Long parseTimestamp(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant().getEpochSecond();
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LinkPreviewInfo queryLinkPreviewNet(String url) {
        LinkPreviewNetInfo info;
        try {
            info = linkPreviewNet.query(url);
        } catch (LinkPreviewNetException e) {
            throw new OperationFailure("proxy.request-failed");
        }

        LinkPreviewInfo linkPreviewInfo = new LinkPreviewInfo();
        linkPreviewInfo.setUrl(info.getUrl());
        linkPreviewInfo.setTitle(info.getTitle());
        linkPreviewInfo.setDescription(info.getDescription());
        linkPreviewInfo.setImageUrl(info.getImage());
        return linkPreviewInfo;
    }

}
