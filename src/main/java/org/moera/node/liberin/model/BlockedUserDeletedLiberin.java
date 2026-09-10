package org.moera.node.liberin.model;

import org.moera.node.data.BlockedUser;
import org.moera.node.liberin.Liberin;

public class BlockedUserDeletedLiberin extends Liberin {

    private BlockedUser blockedUser;

    public BlockedUserDeletedLiberin(BlockedUser blockedUser) {
        this.blockedUser = blockedUser;
    }

    public BlockedUser getBlockedUser() {
        return blockedUser;
    }

    public void setBlockedUser(BlockedUser blockedUser) {
        this.blockedUser = blockedUser;
    }

}
