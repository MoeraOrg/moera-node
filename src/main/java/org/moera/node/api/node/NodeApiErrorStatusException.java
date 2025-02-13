package org.moera.node.api.node;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.Result;
import org.moera.lib.util.LogUtil;

public class NodeApiErrorStatusException extends NodeApiException {

    private Result result;

    protected NodeApiErrorStatusException(String message) {
        super(message);
    }

    public NodeApiErrorStatusException(int status, String body) {
        super(String.format("Error status returned: %d (body: %s)", status, LogUtil.format(body)));

        try {
            result = new ObjectMapper().readValue(body, Result.class);
        } catch (IOException ignored) {
        }
    }

    public Result getResult() {
        return result;
    }

}
