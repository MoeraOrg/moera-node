package org.moera.node.model;

import javax.validation.constraints.Size;

public class AskDescription {

    private AskSubject subject;

    @Size(max = 40)
    private String friendGroupId;

    @Size(max = 70)
    private String message;

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
