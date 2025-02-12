package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.BlockedUserInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.BlockedUserInfoUtil;
import org.springframework.data.util.Pair;

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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(blockedUser.getId())));
        parameters.add(Pair.of("blockedOperation", LogUtil.format(blockedUser.getBlockedOperation().getValue())));
        parameters.add(Pair.of("nodeName", LogUtil.format(blockedUser.getNodeName())));
        parameters.add(Pair.of("entryId", LogUtil.format(blockedUser.getEntryId())));
        parameters.add(Pair.of("entryNodeName", LogUtil.format(blockedUser.getEntryNodeName())));
        parameters.add(Pair.of("entryPostingId", LogUtil.format(blockedUser.getEntryPostingId())));
        parameters.add(Pair.of("deadline", LogUtil.format(blockedUser.getDeadline())));
    }

}
