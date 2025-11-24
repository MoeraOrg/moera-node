package org.moera.node.mail;

import java.util.Map;

import org.moera.lib.naming.NodeName;

public class DeleteNodeCancelledMail extends Mail {

    private String nodeName;

    public DeleteNodeCancelledMail(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    String getTemplateName() {
        return "delete-node-cancelled";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
            "nodeName", NodeName.shorten(getNodeName()),
            "domainName", getDomainName()
        );
    }

}
