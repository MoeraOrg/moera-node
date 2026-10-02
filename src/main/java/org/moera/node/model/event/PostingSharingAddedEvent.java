package org.moera.node.model.event;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SharingInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;

public class PostingSharingAddedEvent extends Event {

    private String postingId;
    private SharingInfo sharing;

    public PostingSharingAddedEvent() {
        super(EventType.POSTING_SHARING_ADDED, Scope.VIEW_CONTENT);
    }

    public PostingSharingAddedEvent(UUID postingId, SharingInfo sharing, PrincipalFilter filter) {
        super(EventType.POSTING_SHARING_ADDED, Scope.VIEW_CONTENT, filter);
        this.postingId = postingId.toString();
        this.sharing = sharing;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public SharingInfo getSharing() {
        return sharing;
    }

    public void setSharing(SharingInfo sharing) {
        this.sharing = sharing;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("postingId", LogUtil.format(postingId)));
        parameters.add(new LogParameter("sharingId", LogUtil.format(sharing != null ? sharing.getId() : null)));
    }

}
