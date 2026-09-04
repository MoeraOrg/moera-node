package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffComplaintGroupInfo;
import org.moera.lib.node.types.SheriffComplaintStatus;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.model.SheriffComplaintGroupInfoUtil;

public class SheriffComplaintGroupEvent extends Event {

    private SheriffComplaintGroupInfo group;

    protected SheriffComplaintGroupEvent(EventType type) {
        super(type, Scope.SHERIFF, Principal.ADMIN);
    }

    protected SheriffComplaintGroupEvent(EventType type, SheriffComplaintGroup group) {
        super(type, Scope.SHERIFF, Principal.ADMIN);
        this.group = SheriffComplaintGroupInfoUtil.build(group);
    }

    public SheriffComplaintGroupInfo getGroup() {
        return group;
    }

    public void setGroup(SheriffComplaintGroupInfo group) {
        this.group = group;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(group.getId())));
        parameters.add(new LogParameter("remoteNodeName", LogUtil.format(group.getRemoteNodeName())));
        parameters.add(new LogParameter("remoteFeedName", LogUtil.format(group.getRemoteFeedName())));
        parameters.add(new LogParameter("remotePostingOwnerName", LogUtil.format(group.getRemotePostingOwnerName())));
        parameters.add(new LogParameter("remotePostingId", LogUtil.format(group.getRemotePostingId())));
        parameters.add(new LogParameter("remotePostingRevisionId", LogUtil.format(group.getRemotePostingRevisionId())));
        parameters.add(new LogParameter("remoteCommentOwnerName", LogUtil.format(group.getRemoteCommentOwnerName())));
        parameters.add(new LogParameter("remoteCommentId", LogUtil.format(group.getRemoteCommentId())));
        parameters.add(new LogParameter("remoteCommentRevisionId", LogUtil.format(group.getRemoteCommentRevisionId())));
        parameters.add(new LogParameter("status", LogUtil.format(SheriffComplaintStatus.toValue(group.getStatus()))));
        parameters.add(new LogParameter(
            "decisionCode", LogUtil.format(SheriffOrderReason.toValue(group.getDecisionCode()))
        ));
    }

}
