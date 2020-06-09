package org.moera.node.api;

public class NodeApiValidationException extends NodeApiErrorStatusException {

    private String errorCode;

    public NodeApiValidationException(String errorCode) {
        super("Validation failed: " + errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
