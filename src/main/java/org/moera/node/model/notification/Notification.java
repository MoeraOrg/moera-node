package org.moera.node.model.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Notification {

    @JsonIgnore
    private String senderNodeName;

    @JsonIgnore
    private String receiverNodeName;

    @JsonIgnore
    private NotificationType type;

    protected Notification(NotificationType type) {
        this.type = type;
    }

    public String getSenderNodeName() {
        return senderNodeName;
    }

    public void setSenderNodeName(String senderNodeName) {
        this.senderNodeName = senderNodeName;
    }

    public String getReceiverNodeName() {
        return receiverNodeName;
    }

    public void setReceiverNodeName(String receiverNodeName) {
        this.receiverNodeName = receiverNodeName;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

}
