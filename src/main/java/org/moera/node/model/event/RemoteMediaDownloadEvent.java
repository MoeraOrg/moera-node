package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

public abstract class RemoteMediaDownloadEvent extends Event {

    private String nodeName;
    private String mediaId;

    protected RemoteMediaDownloadEvent(EventType type) {
        super(type, Scope.VIEW_CONTENT, Principal.ADMIN);
    }

    protected RemoteMediaDownloadEvent(EventType type, String nodeName, String mediaId) {
        super(type, Scope.VIEW_CONTENT, Principal.ADMIN);
        this.nodeName = nodeName;
        this.mediaId = mediaId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("nodeName", LogUtil.format(nodeName)));
        parameters.add(new LogParameter("mediaId", LogUtil.format(mediaId)));
    }

}
