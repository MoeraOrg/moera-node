package org.moera.node.domain;

public class DomainNotSetException extends Exception {

    public DomainNotSetException() {
        super("Domain name is not set. Set node.domain in the configuration file");
    }

    public DomainNotSetException(String message) {
        super(message);
    }

}
