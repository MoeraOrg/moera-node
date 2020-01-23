package org.moera.node.event.model;

import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.event.EventSubscriber;

public abstract class RemoteReactionVerificationEvent extends Event {

    private String id;
    private String nodeName;
    private String postingId;
    private String reactionOwnerName;

    protected RemoteReactionVerificationEvent(EventType type) {
        super(type);
    }

    protected RemoteReactionVerificationEvent(EventType type, RemoteReactionVerification data) {
        super(type);
        id = data.getId().toString();
        nodeName = data.getNodeName();
        postingId = data.getPostingId();
        reactionOwnerName = data.getReactionOwnerName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getReactionOwnerName() {
        return reactionOwnerName;
    }

    public void setReactionOwnerName(String reactionOwnerName) {
        this.reactionOwnerName = reactionOwnerName;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
