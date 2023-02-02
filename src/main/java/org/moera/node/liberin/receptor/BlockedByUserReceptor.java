package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.BlockedUserInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.BlockedByUserAddedLiberin;
import org.moera.node.liberin.model.BlockedByUserDeletedLiberin;

@LiberinReceptor
public class BlockedByUserReceptor extends LiberinReceptorBase {

    @Inject
    private BlockedUserInstants blockedUserInstants;

    @LiberinMapping
    public void added(BlockedByUserAddedLiberin liberin) {
        blockedUserInstants.blocked(liberin.getBlockedByUser());
    }

    @LiberinMapping
    public void deleted(BlockedByUserDeletedLiberin liberin) {
        blockedUserInstants.unblocked(liberin.getBlockedByUser());
    }

}
