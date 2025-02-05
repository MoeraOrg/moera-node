package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.RemoteCommentVerification;
import org.springframework.data.util.Pair;

public abstract class RemoteCommentVerificationEvent extends Event {

    private String id;
    private String nodeName;
    private String postingId;
    private String commentId;

    protected RemoteCommentVerificationEvent(EventType type) {
        super(type, Scope.OTHER, Principal.ADMIN);
    }

    protected RemoteCommentVerificationEvent(EventType type, RemoteCommentVerification data) {
        super(type, Scope.OTHER, Principal.ADMIN);
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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(nodeName)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("commentId", LogUtil.format(commentId)));
    }

}
