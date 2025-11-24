package org.moera.node.mail;

import java.util.Map;
import java.util.UUID;

public class ComplaintAddedMail extends Mail {

    private UUID id;

    public ComplaintAddedMail(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    String getTemplateName() {
        return "complaint-added";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
            "domainName", getDomainName(),
            "id", id.toString()
        );
    }

}
