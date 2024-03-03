package org.moera.node.api.naming;

public class NamingNotAvailableException extends RuntimeException {

    public NamingNotAvailableException(Throwable e) {
        super("Naming server is not available", e);
    }

}
