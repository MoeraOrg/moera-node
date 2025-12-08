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
    boolean verifiedAddressOnly() {
        return false;
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
            "nodeName", nodeName != null ? NodeName.shorten(nodeName) : getDomainName(),
            "domainName", getDomainName(),
            "email", getEmail(),
            "token", token
        );
    }

}
