package org.moera.node.linkpreviewnet;

import java.io.IOException;
import java.time.Duration;
import jakarta.inject.Inject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.moera.node.config.Config;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class LinkPreviewNet {

    private static final Duration CALL_API_CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CALL_API_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    private static final String API_ENDPOINT = "https://api.linkpreview.net";

    @Inject
    private Config config;

    @Inject
    private ObjectMapper objectMapper;

    private OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(CALL_API_CONNECTION_TIMEOUT)
            .callTimeout(CALL_API_REQUEST_TIMEOUT)
            .build();
    }

    private Request buildRequest(String url) {
        RequestBody requestBody = new FormBody.Builder()
            .add("q", url)
            .build();

        return new Request.Builder()
            .url(API_ENDPOINT)
            .header("X-Linkpreview-Api-Key", config.getLinkPreview().getServiceKey())
            .header(HttpHeaders.USER_AGENT, config.getUserAgent())
            .post(requestBody)
            .build();
    }

    public LinkPreviewNetInfo query(String url) throws LinkPreviewNetException {
        Request request = buildRequest(url);
        try {
            try (Response response = buildClient().newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                String responseText = responseBody != null ? responseBody.string() : "";
                int statusCode = response.code();
                if (statusCode == HttpStatus.OK.value()) {
                    try {
                        return objectMapper.readValue(responseText, LinkPreviewNetInfo.class);
                    } catch (JacksonException e) {
                        throw new LinkPreviewNetException("Error parsing API response", e);
                    }
                } else {
                    try {
                        LinkPreviewNetInfo info = objectMapper.readValue(responseText, LinkPreviewNetInfo.class);
                        int errorCode = info.getError() != null ? info.getError() : statusCode;
                        throw new LinkPreviewNetException(
                            "Error returned (%d): %s".formatted(errorCode, info.getDescription())
                        );
                    } catch (JacksonException e) {
                        throw new LinkPreviewNetException("Error status returned: " + statusCode);
                    }
                }
            }
        } catch (IOException e) {
            throw new LinkPreviewNetException("Connection error", e);
        }
    }

}
