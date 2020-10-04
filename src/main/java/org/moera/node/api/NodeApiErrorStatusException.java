package org.moera.node.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.model.Result;

public class NodeApiErrorStatusException extends NodeApiException {

    private Result result;

    protected NodeApiErrorStatusException(String message) {
        super(message);
    }

    public NodeApiErrorStatusException(int status, String body) {
        super(String.format("Error status returned: %d (body: %s)", status, body));

        try {
            result = new ObjectMapper().readValue(body, Result.class);
        } catch (IOException ignored) {
        }
    }

    public Result getResult() {
        return result;
    }

}
