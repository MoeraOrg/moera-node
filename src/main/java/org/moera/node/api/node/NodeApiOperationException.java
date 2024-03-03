package org.moera.node.api.node;

public class NodeApiOperationException extends NodeApiErrorStatusException {

    private String errorCode;

    public NodeApiOperationException(String errorCode) {
        super("Validation failed: " + errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
