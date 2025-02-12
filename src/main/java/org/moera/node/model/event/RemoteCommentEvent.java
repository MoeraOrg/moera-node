package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class RemoteCommentEvent extends Event {

    private String remoteNodeName;
    private String remotePostingId;
    private String remoteCommentId;

    protected RemoteCommentEvent(EventType type) {
        super(type, Scope.VIEW_CONTENT, Principal.ADMIN);
    }

    protected RemoteCommentEvent(EventType type, String remoteNodeName, String remotePostingId,
                                 String remoteCommentId) {
        super(type, Scope.VIEW_CONTENT, Principal.ADMIN);
        this.remoteNodeName = remoteNodeName;
        this.remotePostingId = remotePostingId;
        this.remoteCommentId = remoteCommentId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getRemoteCommentId() {
        return remoteCommentId;
    }

    public void setRemoteCommentId(String remoteCommentId) {
        this.remoteCommentId = remoteCommentId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(remotePostingId)));
        parameters.add(Pair.of("remoteCommentId", LogUtil.format(remoteCommentId)));
    }

}
