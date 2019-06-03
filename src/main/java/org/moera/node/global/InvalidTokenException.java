package org.moera.node.global;

public class InvalidTokenException extends Exception {

    public InvalidTokenException() {
        super("Authentication token is invalid");
    }

}
