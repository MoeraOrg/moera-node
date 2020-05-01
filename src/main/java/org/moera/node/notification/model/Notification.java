package org.moera.node.notification.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Notification {

    @JsonIgnore
    private String senderNodeName;

    public String getSenderNodeName() {
        return senderNodeName;
    }

    public void setSenderNodeName(String senderNodeName) {
        this.senderNodeName = senderNodeName;
    }

}
