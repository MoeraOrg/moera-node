package org.moera.node.event.model;

import org.moera.node.data.RemotePostingVerification;
import org.moera.node.event.EventSubscriber;

public abstract class RemotePostingEvent extends Event {

    private String nodeName;
    private String id;
    private String revisionId;

    protected RemotePostingEvent(EventType type) {
        super(type);
    }

    protected RemotePostingEvent(EventType type, RemotePostingVerification data) {
        super(type);
        nodeName = data.getNodeName();
        id = data.getPostingId();
        revisionId = data.getRevisionId();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
