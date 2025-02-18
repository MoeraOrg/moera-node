package org.moera.node.model.notification;

import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.AskDescription;
import org.moera.lib.node.types.AskSubject;

public class AskedNotification extends Notification {

    private AskSubject subject;

    @Size(max = 40)
    private String friendGroupId;

    @Size(max = 70)
    private String message;

    public AskedNotification() {
        super(NotificationType.ASKED);
    }

    public AskedNotification(AskDescription askDescription) {
        super(NotificationType.ASKED);
        subject = askDescription.getSubject();
        friendGroupId = askDescription.getFriendGroupId();
        message = askDescription.getMessage();
    }

    public AskSubject getSubject() {
        return subject;
    }

    public void setSubject(AskSubject subject) {
        this.subject = subject;
    }

    public String getFriendGroupId() {
        return friendGroupId;
    }

    public void setFriendGroupId(String friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
