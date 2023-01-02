package org.moera.node.mail;

import java.util.Map;

public class DomainCreatedMail extends Mail {

    private String nodeName;

    public DomainCreatedMail(String nodeName) {
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
        return "domain-created";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
                "domainName", getDomainName(),
                "nodeName", getNodeName()
        );
    }

}
