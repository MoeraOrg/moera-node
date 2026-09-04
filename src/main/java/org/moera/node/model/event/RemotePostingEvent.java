package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

public class RemotePostingEvent extends Event {

    private String remoteNodeName;
    private String remotePostingId;

    protected RemotePostingEvent(EventType type) {
        super(type, Scope.VIEW_CONTENT, Principal.ADMIN);
    }

    protected RemotePostingEvent(EventType type, String remoteNodeName, String remotePostingId) {
        super(type, Scope.VIEW_CONTENT, Principal.ADMIN);
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
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(new LogParameter("remotePostingId", LogUtil.format(remotePostingId)));
    }

}
