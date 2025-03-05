package org.moera.node.option.exception;

public class UnknownOptionTypeException extends RuntimeException {

    public UnknownOptionTypeException(String type) {
        super("Unknown option type '%s'".formatted(type));
    }

}
