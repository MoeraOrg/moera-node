package org.moera.node.model.event;

import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.event.EventSubscriber;

public abstract class RemoteCommentVerificationEvent extends Event {

    private String id;
    private String nodeName;
    private String postingId;
    private String commentId;

    protected RemoteCommentVerificationEvent(EventType type) {
        super(type);
    }

    protected RemoteCommentVerificationEvent(EventType type, RemoteCommentVerification data) {
        super(type);
        id = data.getId().toString();
        nodeName = data.getNodeName();
        postingId = data.getPostingId();
        commentId = data.getCommentId();
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
