package org.moera.node.model;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OperationFailure extends RuntimeException implements MessageSourceResolvable {

    private String errorCode;

    protected OperationFailure(String message, String errorCode) {
        super(message + ": " + errorCode);
        this.errorCode = errorCode;
    }

    public OperationFailure(String errorCode) {
        this("Operation failed", errorCode);
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
        return getMessage();
    }

}
