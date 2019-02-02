package org.moera.node.option;

public class NodeIdNotSetException extends Exception {

    public NodeIdNotSetException() {
        super("node.id is not set in the configuration");
    }

}
