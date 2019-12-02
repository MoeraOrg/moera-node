package org.moera.node.model;

public class BodyMappingException extends RuntimeException {

    public BodyMappingException() {
        super("Error mapping a Body object");
    }

    public BodyMappingException(Throwable cause) {
        super("Error mapping a Body object", cause);
    }

}
