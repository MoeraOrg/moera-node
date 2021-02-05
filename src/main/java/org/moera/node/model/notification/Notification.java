package org.moera.node.model.notification;

import java.sql.Timestamp;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.util.Util;

public abstract class Notification implements Cloneable {

    @JsonIgnore
    private String senderNodeName;

    @JsonIgnore
    private String senderFullName;

    @JsonIgnore
    private NotificationType type;

    @JsonIgnore
    private UUID pendingNotificationId;

    @JsonIgnore
    private Timestamp createdAt = Util.now();

    protected Notification(NotificationType type) {
        this.type = type;
    }

    public String getSenderNodeName() {
        return senderNodeName;
    }

    public void setSenderNodeName(String senderNodeName) {
        this.senderNodeName = senderNodeName;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public void setSenderFullName(String senderFullName) {
        this.senderFullName = senderFullName;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public UUID getPendingNotificationId() {
        return pendingNotificationId;
    }

    public void setPendingNotificationId(UUID pendingNotificationId) {
        this.pendingNotificationId = pendingNotificationId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Notification clone() {
        try {
            return (Notification) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Must implement Cloneable", e);
        }
    }

}
