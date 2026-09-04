package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.BlockedByUserInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.BlockedByUserInfoUtil;

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
        BlockedByUserInfoUtil.protect(blockedByUser, eventSubscriber);
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(blockedByUser.getId())));
        parameters.add(new LogParameter(
            "blockedOperation", LogUtil.format(blockedByUser.getBlockedOperation().getValue())
        ));
        parameters.add(new LogParameter("nodeName", LogUtil.format(blockedByUser.getNodeName())));
        parameters.add(new LogParameter("postingId", LogUtil.format(blockedByUser.getPostingId())));
        parameters.add(new LogParameter("deadline", LogUtil.format(blockedByUser.getDeadline())));
    }

}
