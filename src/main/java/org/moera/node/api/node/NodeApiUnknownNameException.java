package org.moera.node.api.node;

public class NodeApiUnknownNameException extends NodeApiException {

    private final String nodeName;

    public NodeApiUnknownNameException(String nodeName) {
        super(String.format("Node name '%s' not found", nodeName));
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

}
