package org.moera.node.media;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import jakarta.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class MediaDownloadOperations {

    private static final Logger log = LoggerFactory.getLogger(MediaDownloadOperations.class);

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(1);

    public record MediaStream(InputStream stream, MediaType contentType, Long contentLength) {
    }

    @Inject
    private Config config;

    public MediaStream fetchMedia(String url) {
        log.info("Fetching remote media from {}", LogUtil.format(url));

        OkHttpClient client = buildClient();
        Request request;
        try {
            request = new Request.Builder()
                .header(HttpHeaders.USER_AGENT, config.getUserAgent())
                .get()
                .url(url)
                .build();
        } catch (IllegalArgumentException e) {
            throw new ValidationFailure("media.url.invalid");
        }
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new OperationFailure("media.download-failed");
        }

        int statusCode = response.code();
        if (statusCode != HttpStatus.OK.value()) {
            response.close();
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                throw new ObjectNotFoundFailure("media.not-found");
            } else {
                throw new OperationFailure("media.download-failed");
            }
        }

        String contentType = response.header(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !MimeUtil.isSupportedImage(contentType)) {
            response.close();
            throw new ValidationFailure("media.image-invalid");
        }
        ResponseBody body = response.body();
        if (body == null) {
            response.close();
            throw new OperationFailure("media.download-failed");
        }
        Long contentLength = body.contentLength();
        if (contentLength < 0) {
            contentLength = null;
        }

        return new MediaStream(body.byteStream(), MediaType.valueOf(contentType), contentLength);
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
            .protocols(List.of(Protocol.HTTP_1_1))
            .followRedirects(true)
            .connectTimeout(CONNECTION_TIMEOUT)
            .callTimeout(REQUEST_TIMEOUT)
            .build();
    }

}
