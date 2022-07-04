package org.moera.node.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.model.LinkPreviewInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
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

    @GetMapping("/media")
    @Admin
    public ResponseEntity<Resource> getMedia(@RequestParam String url) {
        log.info("GET /proxy/media, (url = {})", LogUtil.format(url));

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECTION_TIMEOUT)
                .build();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ValidationFailure("proxy.url.invalid");
        }
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new OperationFailure("proxy.request-failed");
        }

        if (response.statusCode() != HttpStatus.OK.value()) {
            if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ObjectNotFoundFailure("proxy.resource-not-found");
            } else {
                throw new OperationFailure("proxy.error-status");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ValidationFailure("proxy.resource-not-media");
        }
        headers.setContentType(MediaType.valueOf(contentType));
        response.headers().firstValueAsLong("Content-Length").ifPresent(headers::setContentLength);
        return new ResponseEntity<>(new InputStreamResource(response.body()), headers, HttpStatus.OK);
    }

    @GetMapping("/link-preview")
    @Admin
    public LinkPreviewInfo getLinkPreview(@RequestParam String url) {
        log.info("GET /proxy/link-preview, (url = {})", LogUtil.format(url));

        Document document;
        try {
            document = Jsoup.connect(url)
                    .header("User-Agent", "curl/7.68.0")
                    .followRedirects(true)
                    .timeout((int) REQUEST_TIMEOUT.toMillis())
                    .get();
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
            String content = element.attr("content");
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
                case "title":
                    if (ObjectUtils.isEmpty(linkPreviewInfo.getTitle())) {
                        linkPreviewInfo.setTitle(content);
                    }
                    break;
                case "description":
                    if (ObjectUtils.isEmpty(linkPreviewInfo.getDescription())) {
                        linkPreviewInfo.setDescription(content);
                    }
                    break;
                default:
                    // ignore
            }
        }
        if (ObjectUtils.isEmpty(linkPreviewInfo.getTitle())) {
            Elements titles = document.select("head title");
            for (Element title : titles) {
                linkPreviewInfo.setTitle(title.ownText());
            }
        }
        return linkPreviewInfo;
    }

}
