package org.moera.node.global;

public class AuthorizationException extends Exception {

    public AuthorizationException() {
        super("Authorization required");
    }

}
