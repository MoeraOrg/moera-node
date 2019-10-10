package org.moera.node.option.exception;

public class UnknownOptionTypeException extends RuntimeException {

    public UnknownOptionTypeException(String type) {
        super(String.format("Unknown option type '%s'", type));
    }

}
