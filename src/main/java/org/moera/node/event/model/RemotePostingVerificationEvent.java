package org.moera.node.event.model;

import org.moera.node.data.RemotePostingVerification;
import org.moera.node.event.EventSubscriber;

public abstract class RemotePostingVerificationEvent extends Event {

    private String id;
    private String nodeName;
    private String receiverName;
    private String postingId;
    private String revisionId;

    protected RemotePostingVerificationEvent(EventType type) {
        super(type);
    }

    protected RemotePostingVerificationEvent(EventType type, RemotePostingVerification data) {
        super(type);
        id = data.getId().toString();
        nodeName = data.getNodeName();
        receiverName = data.getReceiverName();
        postingId = data.getPostingId();
        revisionId = data.getRevisionId();
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

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
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
