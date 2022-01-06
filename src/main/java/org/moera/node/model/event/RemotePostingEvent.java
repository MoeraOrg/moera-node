package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

public class RemotePostingEvent extends Event {

    private String remoteNodeName;
    private String remotePostingId;

    protected RemotePostingEvent(EventType type) {
        super(type);
    }

    protected RemotePostingEvent(EventType type, String remoteNodeName, String remotePostingId) {
        super(type);
        this.remoteNodeName = remoteNodeName;
        this.remotePostingId = remotePostingId;
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

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(remotePostingId)));
    }

}
