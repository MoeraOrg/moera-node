package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.model.SheriffComplainGroupInfo;
import org.moera.node.model.SheriffOrderReason;
import org.springframework.data.util.Pair;

public class SheriffComplainGroupEvent extends Event {

    private SheriffComplainGroupInfo group;

    protected SheriffComplainGroupEvent(EventType type) {
        super(type, Principal.ADMIN);
    }

    protected SheriffComplainGroupEvent(EventType type, SheriffComplainGroup group) {
        super(type, Principal.ADMIN);
        this.group = new SheriffComplainGroupInfo(group);
    }

    public SheriffComplainGroupInfo getGroup() {
        return group;
    }

    public void setGroup(SheriffComplainGroupInfo group) {
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
        parameters.add(Pair.of("status", LogUtil.format(SheriffComplainStatus.toValue(group.getStatus()))));
        parameters.add(Pair.of("decisionCode", LogUtil.format(SheriffOrderReason.toValue(group.getDecisionCode()))));
    }

}
