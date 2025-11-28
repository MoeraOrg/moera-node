package org.moera.node.mail;

import java.util.Collections;
import java.util.Map;

public abstract class Mail {

    private String domainName;
    private String email;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    abstract String getTemplateName();

    boolean verifiedAddressOnly() {
        return true;
    }

    Map<String, Object> getModel() {
        return Collections.emptyMap();
    }

}
