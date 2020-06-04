package org.moera.node.task;

public class CallApiErrorStatusException extends CallApiException {

    protected CallApiErrorStatusException(String message) {
        super(message);
    }

    public CallApiErrorStatusException(int status, String body) {
        super(String.format("Error status returned: %d (body: %s)", status, body));
    }

}
