package org.moera.node.model.event;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;

public class PostingSharingDeletedEvent extends Event {

    private String postingId;
    private String sharingId;

    public PostingSharingDeletedEvent() {
        super(EventType.POSTING_SHARING_DELETED, Scope.VIEW_CONTENT);
    }

    public PostingSharingDeletedEvent(UUID postingId, UUID sharingId, PrincipalFilter filter) {
        super(EventType.POSTING_SHARING_DELETED, Scope.VIEW_CONTENT, filter);
        this.postingId = postingId.toString();
        this.sharingId = sharingId.toString();
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getSharingId() {
        return sharingId;
    }

    public void setSharingId(String sharingId) {
        this.sharingId = sharingId;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("postingId", LogUtil.format(postingId)));
        parameters.add(new LogParameter("sharingId", LogUtil.format(sharingId)));
    }

}
