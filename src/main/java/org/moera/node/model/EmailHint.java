package org.moera.node.model;

import org.springframework.util.ObjectUtils;

public class EmailHint {

    private String emailHint;

    public EmailHint(String email) {
        emailHint = buildHint(email);
    }

    private String buildHint(String email) {
        if (ObjectUtils.isEmpty(email)) {
            return email;
        }
        String[] parts = email.split("@");
        StringBuilder buf = new StringBuilder();
        if (parts.length > 0 && parts[0] != null && parts[0].length() > 0) {
            buf.append(parts[0].charAt(0));
            buf.append("***");
        }
        buf.append('@');
        if (parts.length > 1 && parts[1] != null && parts[1].length() > 0) {
            buf.append(parts[1].charAt(0));
            buf.append("***");
        }
        return buf.toString();
    }

    public String getEmailHint() {
        return emailHint;
    }

    public void setEmailHint(String emailHint) {
        this.emailHint = emailHint;
    }

}
