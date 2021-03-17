package org.moera.node.mail;

import java.util.Collections;
import java.util.Map;

public class EmailConfirmMail extends Mail {

    @Override
    String getTemplateName() {
        return "email-confirm";
    }

    @Override
    Map<String, Object> getModel() {
        return Collections.singletonMap("domainName", getDomainName());
    }

}
