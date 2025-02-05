package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.BlockedByUserInfo;
import org.springframework.data.util.Pair;

public class BlockedByUserEvent extends Event {

    private BlockedByUserInfo blockedByUser;

    protected BlockedByUserEvent(EventType type) {
        super(type, Scope.VIEW_PEOPLE);
    }

    protected BlockedByUserEvent(EventType type, BlockedByUserInfo blockedByUser, PrincipalFilter filter) {
        super(type, Scope.VIEW_PEOPLE, filter);
        this.blockedByUser = blockedByUser;
    }

    public BlockedByUserInfo getBlockedByUser() {
        return blockedByUser;
    }

    public void setBlockedByUser(BlockedByUserInfo blockedByUser) {
        this.blockedByUser = blockedByUser;
    }

    @Override
    public void protect(EventSubscriber eventSubscriber) {
        blockedByUser.protect(eventSubscriber);
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(blockedByUser.getId())));
        parameters.add(Pair.of("blockedOperation", LogUtil.format(blockedByUser.getBlockedOperation().getValue())));
        parameters.add(Pair.of("nodeName", LogUtil.format(blockedByUser.getNodeName())));
        parameters.add(Pair.of("postingId", LogUtil.format(blockedByUser.getPostingId())));
        parameters.add(Pair.of("deadline", LogUtil.format(blockedByUser.getDeadline())));
    }

}
