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
import org.springframework.data.util.Pair;

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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(group.getId())));
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(group.getRemoteNodeName())));
        parameters.add(Pair.of("remoteFeedName", LogUtil.format(group.getRemoteFeedName())));
        parameters.add(Pair.of("remotePostingOwnerName", LogUtil.format(group.getRemotePostingOwnerName())));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(group.getRemotePostingId())));
        parameters.add(Pair.of("remotePostingRevisionId", LogUtil.format(group.getRemotePostingRevisionId())));
        parameters.add(Pair.of("remoteCommentOwnerName", LogUtil.format(group.getRemoteCommentOwnerName())));
        parameters.add(Pair.of("remoteCommentId", LogUtil.format(group.getRemoteCommentId())));
        parameters.add(Pair.of("remoteCommentRevisionId", LogUtil.format(group.getRemoteCommentRevisionId())));
        parameters.add(Pair.of("status", LogUtil.format(SheriffComplaintStatus.toValue(group.getStatus()))));
        parameters.add(Pair.of("decisionCode", LogUtil.format(SheriffOrderReason.toValue(group.getDecisionCode()))));
    }

}
