package org.moera.node.option.exception;

import org.springframework.context.MessageSourceResolvable;

public abstract class OptionValueException extends RuntimeException implements MessageSourceResolvable {

    private String errorCode;

    protected OptionValueException(String message, String errorCode) {
        super(message);
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
