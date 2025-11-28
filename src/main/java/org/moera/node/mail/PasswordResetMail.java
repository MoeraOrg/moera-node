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
    boolean verifiedAddressOnly() {
        // Yes, it should be sent only to verified addresses. But users will forget to verify addresses regularly
        // and then complain. Let's do their lives easier. E-mail address verification is an antispam feature, but it
        // is not possible to send spam with password reset emails. As a security feature it will not help if you put
        // the attacker's e-mail address to your profile.
        return false;
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
            "domainName", getDomainName(),
            "token", token
        );
    }

}
