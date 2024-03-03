package org.moera.node.api.node;

public class NodeApiUnknownNameException extends NodeApiException {

    public NodeApiUnknownNameException(String nodeName) {
        super(String.format("Node name '%s' not found", nodeName));
    }

}
