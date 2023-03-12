package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.BlockedByUser;
import org.moera.node.instant.BlockedUserInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.BlockedByUserAddedLiberin;
import org.moera.node.liberin.model.BlockedByUserDeletedLiberin;
import org.moera.node.model.BlockedByUserInfo;
import org.moera.node.model.event.BlockedByUserAddedEvent;
import org.moera.node.model.event.BlockedByUserDeletedEvent;

@LiberinReceptor
public class BlockedByUserReceptor extends LiberinReceptorBase {

    @Inject
    private BlockedUserInstants blockedUserInstants;

    @LiberinMapping
    public void added(BlockedByUserAddedLiberin liberin) {
        BlockedByUser blockedByUser = liberin.getBlockedByUser();
        blockedUserInstants.blocked(blockedByUser, liberin.getEntryHeading());
        send(liberin, new BlockedByUserAddedEvent(
                new BlockedByUserInfo(blockedByUser, universalContext.getOptions(), AccessCheckers.ADMIN)));
    }

    @LiberinMapping
    public void deleted(BlockedByUserDeletedLiberin liberin) {
        BlockedByUser blockedByUser = liberin.getBlockedByUser();
        blockedUserInstants.unblocked(blockedByUser, liberin.getEntryHeading());
        send(liberin, new BlockedByUserDeletedEvent(
                new BlockedByUserInfo(blockedByUser, universalContext.getOptions(), AccessCheckers.ADMIN)));
    }

}
