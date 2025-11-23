package org.moera.node.mail;

import java.util.Map;

public class PasswordResetMail extends Mail {

    private final String token;

    public PasswordResetMail(String token) {
        this.token = token;
    }

    @Override
    String getTemplateName() {
        return "password-reset";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
            "domainName", getDomainName(),
            "token", token
        );
    }

}
