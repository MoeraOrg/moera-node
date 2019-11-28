package org.moera.node.event.model;

import java.util.UUID;

import org.moera.node.data.RemotePostingVerification;
import org.moera.node.event.EventSubscriber;

public abstract class RemotePostingVerificationEvent extends Event {

    private UUID id;
    private String nodeName;
    private String postingId;
    private String revisionId;

    protected RemotePostingVerificationEvent(EventType type) {
        super(type);
    }

    protected RemotePostingVerificationEvent(EventType type, RemotePostingVerification data) {
        super(type);
        id = data.getId();
        nodeName = data.getNodeName();
        postingId = data.getPostingId();
        revisionId = data.getRevisionId();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
