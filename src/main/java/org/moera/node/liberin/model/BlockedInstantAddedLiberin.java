package org.moera.node.liberin.model;

import org.moera.node.data.BlockedInstant;
import org.moera.node.liberin.Liberin;

public class BlockedInstantAddedLiberin extends Liberin {

    private BlockedInstant blockedInstant;

    public BlockedInstantAddedLiberin(BlockedInstant blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

    public BlockedInstant getBlockedInstant() {
        return blockedInstant;
    }

    public void setBlockedInstant(BlockedInstant blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

}
