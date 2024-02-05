package org.moera.node.mail;

import java.util.Map;
import java.util.Objects;

public class DeleteNodeMail extends Mail {

    private String nodeName;
    private String message;

    public DeleteNodeMail(String nodeName, String message) {
        this.nodeName = nodeName;
        this.message = message;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    String getTemplateName() {
        return "delete-node";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
                "nodeName", getNodeName(),
                "domainName", getDomainName(),
                "email", getEmail(),
                "adminMessage", Objects.toString(getMessage(), "")
        );
    }

}
