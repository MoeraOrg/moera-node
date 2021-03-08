package org.moera.node.auth;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException() {
        super("Authentication required");
    }

}
