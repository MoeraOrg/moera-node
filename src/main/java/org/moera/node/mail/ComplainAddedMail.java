package org.moera.node.mail;

import java.util.Map;
import java.util.UUID;

public class ComplainAddedMail extends Mail {

    private UUID id;

    public ComplainAddedMail(UUID id) {
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
        return "complain-added";
    }

    @Override
    Map<String, Object> getModel() {
        return Map.of(
                "domainName", getDomainName(),
                "id", id.toString()
        );
    }

}
