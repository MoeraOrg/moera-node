package org.moera.node.global;

public class AuthenticationException extends Exception {

    public AuthenticationException() {
        super("Authentication required");
    }

}
