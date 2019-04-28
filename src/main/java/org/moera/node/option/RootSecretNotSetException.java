package org.moera.node.option;

public class RootSecretNotSetException extends Exception {

    public RootSecretNotSetException() {
        super("node.root-secret is not set in the configuration");
    }

}
