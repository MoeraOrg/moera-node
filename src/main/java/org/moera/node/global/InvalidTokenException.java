package org.moera.node.global;

public class InvalidTokenException extends Exception {

    public InvalidTokenException() {
        super("Authorization token is invalid");
    }

}
