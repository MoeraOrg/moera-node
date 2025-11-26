package org.moera.node.mail;

import java.util.Map;

import org.moera.lib.naming.NodeName;

public class EmailConfirmMail extends Mail {

    private final String nodeName;
    private final String token;

    public EmailConfirmMail(String nodeName, String token) {
        this.nodeName = nodeName;
        this.token = token;
    }

    @Override
    String getTemplateName() {
        return "email-confirm";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
            "nodeName", NodeName.shorten(nodeName),
            "domainName", getDomainName(),
            "email", getEmail(),
            "token", token
        );
    }

}
