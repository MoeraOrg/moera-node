package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.BlockedByUserInfo;
import org.springframework.data.util.Pair;

public class BlockedByUserEvent extends Event {

    private BlockedByUserInfo blockedByUser;

    protected BlockedByUserEvent(EventType type) {
        super(type, Principal.ADMIN);
    }

    protected BlockedByUserEvent(EventType type, BlockedByUserInfo blockedByUser) {
        super(type, Principal.ADMIN);
        this.blockedByUser = blockedByUser;
    }

    public BlockedByUserInfo getBlockedByUser() {
        return blockedByUser;
    }

    public void setBlockedByUser(BlockedByUserInfo blockedByUser) {
        this.blockedByUser = blockedByUser;
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
