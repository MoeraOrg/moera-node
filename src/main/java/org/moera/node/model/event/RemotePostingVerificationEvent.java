package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.RemotePostingVerification;
import org.springframework.data.util.Pair;

public abstract class RemotePostingVerificationEvent extends Event {

    private String id;
    private String nodeName;
    private String receiverName;
    private String postingId;
    private String revisionId;

    protected RemotePostingVerificationEvent(EventType type) {
        super(type, Scope.OTHER, Principal.ADMIN);
    }

    protected RemotePostingVerificationEvent(EventType type, RemotePostingVerification data) {
        super(type, Scope.OTHER, Principal.ADMIN);
        id = data.getId().toString();
        nodeName = data.getNodeName();
        receiverName = data.getOwnerName();
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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(nodeName)));
        parameters.add(Pair.of("receiverName", LogUtil.format(receiverName)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("revisionId", LogUtil.format(revisionId)));
    }

}
