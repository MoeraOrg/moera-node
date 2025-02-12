package org.moera.node.model;

import org.moera.lib.node.types.EmailHint;
import org.springframework.util.ObjectUtils;

public class EmailHintUtil {

    public static EmailHint build(String email) {
        EmailHint emailHint = new EmailHint();
        if (ObjectUtils.isEmpty(email)) {
            emailHint.setEmailHint(email);
            return emailHint;
        }
        String[] parts = email.split("@");
        StringBuilder buf = new StringBuilder();
        if (parts.length > 0 && parts[0] != null && !parts[0].isEmpty()) {
            buf.append(parts[0].charAt(0));
            buf.append("***");
        }
        buf.append('@');
        if (parts.length > 1 && parts[1] != null && !parts[1].isEmpty()) {
            buf.append(parts[1].charAt(0));
            buf.append("***");
        }
        emailHint.setEmailHint(buf.toString());
        return emailHint;
    }

}
