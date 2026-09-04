package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.BlockedUserInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.BlockedUserInfoUtil;

public class BlockedUserEvent extends Event {

    private BlockedUserInfo blockedUser;

    protected BlockedUserEvent(EventType type) {
        super(type, Scope.VIEW_PEOPLE);
    }

    protected BlockedUserEvent(EventType type, BlockedUserInfo blockedUser, PrincipalFilter filter) {
        super(type, Scope.VIEW_PEOPLE, filter);
        this.blockedUser = blockedUser;
    }

    public BlockedUserInfo getBlockedUser() {
        return blockedUser;
    }

    public void setBlockedUser(BlockedUserInfo blockedUser) {
        this.blockedUser = blockedUser;
    }

    @Override
    public void protect(EventSubscriber eventSubscriber) {
        BlockedUserInfoUtil.protect(blockedUser, eventSubscriber);
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(blockedUser.getId())));
        parameters.add(new LogParameter(
            "blockedOperation", LogUtil.format(blockedUser.getBlockedOperation().getValue())
        ));
        parameters.add(new LogParameter("nodeName", LogUtil.format(blockedUser.getNodeName())));
        parameters.add(new LogParameter("entryId", LogUtil.format(blockedUser.getEntryId())));
        parameters.add(new LogParameter("entryNodeName", LogUtil.format(blockedUser.getEntryNodeName())));
        parameters.add(new LogParameter("entryPostingId", LogUtil.format(blockedUser.getEntryPostingId())));
        parameters.add(new LogParameter("deadline", LogUtil.format(blockedUser.getDeadline())));
    }

}
