package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.BlockedUserInfo;
import org.springframework.data.util.Pair;

public class BlockedUserEvent extends Event {

    private BlockedUserInfo blockedUser;

    protected BlockedUserEvent(EventType type) {
        super(type, Principal.ADMIN);
    }

    protected BlockedUserEvent(EventType type, BlockedUserInfo blockedUser) {
        super(type, Principal.ADMIN);
        this.blockedUser = blockedUser;
    }

    public BlockedUserInfo getBlockedUser() {
        return blockedUser;
    }

    public void setBlockedUser(BlockedUserInfo blockedUser) {
        this.blockedUser = blockedUser;
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
