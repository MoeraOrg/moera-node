package org.moera.node.naming;

public class RegisteredNameParsingException extends RuntimeException {

    public RegisteredNameParsingException(String message) {
        super(message);
    }

    public RegisteredNameParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}
