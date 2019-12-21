package org.moera.node.auth;

public class AuthenticationException extends Exception {

    public AuthenticationException() {
        super("Authentication required");
    }

}
