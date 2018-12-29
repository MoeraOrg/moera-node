package org.moera.node.naming;

public class NamingNotAvailableException extends RuntimeException {

    public NamingNotAvailableException(Throwable e) {
        super("Naming server is not available", e);
    }

}
