package org.moera.node.model;

import org.springframework.context.MessageSourceResolvable;

public class OperationFailure extends RuntimeException implements MessageSourceResolvable {

    private String errorCode;

    public OperationFailure(String errorCode) {
        super("Operation failed: " + errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String[] getCodes() {
        return new String[] {errorCode};
    }

    @Override
    public Object[] getArguments() {
        return new Object[0];
    }

    @Override
    public String getDefaultMessage() {
        return null;
    }

}
