package org.moera.node.api.node;

import org.moera.lib.node.exception.MoeraNodeException;

public class MoeraNodeUnknownNameException extends MoeraNodeException {

    private final String nodeName;

    public MoeraNodeUnknownNameException(String nodeName) {
        super(String.format("Node name '%s' not found", nodeName));
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

}
