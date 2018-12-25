package org.moera.node.option;

public class UnknownOptionTypeException extends RuntimeException {

    public UnknownOptionTypeException(String type) {
        super(String.format("Unknown option type '%s'", type));
    }

}
