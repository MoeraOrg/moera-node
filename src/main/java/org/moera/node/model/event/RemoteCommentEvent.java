package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

public class RemoteCommentEvent extends Event {

    private String remoteNodeName;
    private String remotePostingId;
    private String remoteCommentId;

    protected RemoteCommentEvent(EventType type) {
        super(type);
    }

    protected RemoteCommentEvent(EventType type, String remoteNodeName, String remotePostingId,
                                 String remoteCommentId) {
        super(type);
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
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(remotePostingId)));
        parameters.add(Pair.of("remoteCommentId", LogUtil.format(remoteCommentId)));
    }

}
