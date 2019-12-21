package org.moera.node.auth;

public class InvalidTokenException extends Exception {

    public InvalidTokenException() {
        super("Authentication token is invalid");
    }

}
