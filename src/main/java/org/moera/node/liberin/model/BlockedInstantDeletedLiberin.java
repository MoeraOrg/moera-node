package org.moera.node.liberin.model;

import org.moera.node.data.BlockedInstant;
import org.moera.node.liberin.Liberin;

public class BlockedInstantDeletedLiberin extends Liberin {

    private BlockedInstant blockedInstant;

    public BlockedInstantDeletedLiberin(BlockedInstant blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

    public BlockedInstant getBlockedInstant() {
        return blockedInstant;
    }

    public void setBlockedInstant(BlockedInstant blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

}
