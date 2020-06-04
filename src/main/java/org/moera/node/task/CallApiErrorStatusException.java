package org.moera.node.task;

public class CallApiErrorStatusException extends CallApiException {

    public CallApiErrorStatusException(int status, String body) {
        super(String.format("Error status returned: %d (body: %s)", status, body));
    }

}
