package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.model.notification.NotificationType;
import org.moera.node.util.Util;

@Entity
@Table(name = "pending_notifications")
public class PendingNotification {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String nodeName = "";

    @NotNull
    @Enumerated
    private NotificationType notificationType;

    @NotNull
    private String notification;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp subscriptionCreatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getSubscriptionCreatedAt() {
        return subscriptionCreatedAt;
    }

    public void setSubscriptionCreatedAt(Timestamp subscriptionCreatedAt) {
        this.subscriptionCreatedAt = subscriptionCreatedAt;
    }

}
