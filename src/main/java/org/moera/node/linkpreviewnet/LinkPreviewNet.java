package org.moera.node.linkpreviewnet;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import jakarta.inject.Inject;

import org.moera.node.config.Config;
import org.moera.node.util.Util;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class LinkPreviewNet {

    private static final Duration CALL_API_CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CALL_API_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    private static final URI API_ENDPOINT = URI.create("https://api.linkpreview.net");

    @Inject
    private Config config;

    @Inject
    private ObjectMapper objectMapper;

    private HttpClient buildClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CALL_API_CONNECTION_TIMEOUT)
                .build();
    }

    private HttpRequest buildRequest(String url) {
        String body = "q=" + Util.ue(url);

        return HttpRequest.newBuilder()
                .uri(API_ENDPOINT)
                .timeout(CALL_API_REQUEST_TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("X-Linkpreview-Api-Key", config.getLinkPreview().getServiceKey())
                .header("User-Agent", config.getUserAgent())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    public LinkPreviewNetInfo query(String url) throws LinkPreviewNetException {
        HttpRequest request = buildRequest(url);
        HttpResponse<String> response;
        try {
            response = buildClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new LinkPreviewNetException(e);
        }
        if (response.statusCode() == HttpStatus.OK.value()) {
            try {
                return objectMapper.readValue(response.body(), LinkPreviewNetInfo.class);
            } catch (JacksonException e) {
                throw new LinkPreviewNetException("Error parsing API response", e);
            }
        } else {
            try {
                LinkPreviewNetInfo info = objectMapper.readValue(response.body(), LinkPreviewNetInfo.class);
                int errorCode = info.getError() != null ? info.getError() : response.statusCode();
                throw new LinkPreviewNetException(
                    "Error returned (%d): %s".formatted(errorCode, info.getDescription())
                );
            } catch (JacksonException e) {
                throw new LinkPreviewNetException("Error status returned: " + response.statusCode());
            }
        }
    }

}
