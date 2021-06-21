package org.moera.node.push;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class PushEmitter extends SseEmitter {

    public PushEmitter() {
        super(Long.MAX_VALUE);
    }

    @Override
    protected void extendResponse(ServerHttpResponse outputMessage) {
        super.extendResponse(outputMessage);

        HttpHeaders headers = outputMessage.getHeaders();
        headers.setContentType(new MediaType("text", "event-stream", StandardCharsets.UTF_8));
    }

}
