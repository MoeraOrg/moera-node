package org.moera.node.task;

public class CallApiValidationException extends CallApiErrorStatusException {

    private String errorCode;

    public CallApiValidationException(String errorCode) {
        super("Validation failed: " + errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
