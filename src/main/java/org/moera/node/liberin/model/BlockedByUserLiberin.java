package org.moera.node.liberin.model;

import org.moera.node.data.BlockedByUser;
import org.moera.node.liberin.Liberin;

public class BlockedByUserLiberin extends Liberin {

    private BlockedByUser blockedByUser;
    private String entryHeading;

    public BlockedByUserLiberin(BlockedByUser blockedByUser, String entryHeading) {
        this.blockedByUser = blockedByUser;
        this.entryHeading = entryHeading;
    }

    public BlockedByUser getBlockedByUser() {
        return blockedByUser;
    }

    public void setBlockedByUser(BlockedByUser blockedByUser) {
        this.blockedByUser = blockedByUser;
    }

    public String getEntryHeading() {
        return entryHeading;
    }

    public void setEntryHeading(String entryHeading) {
        this.entryHeading = entryHeading;
    }

}
