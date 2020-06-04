package org.moera.node.task;

public class CallApiException extends Exception {

    public CallApiException(String message) {
        super("API call exception: " + message);
    }

    public CallApiException(String message, Throwable cause) {
        super("API call exception: " + message, cause);
    }

    public CallApiException(Throwable cause) {
        super("API call exception", cause);
    }

}
