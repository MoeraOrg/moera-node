package org.moera.node.liberin.receptor;

import org.moera.node.data.BlockedOperation;
import org.moera.node.data.BlockedUser;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.BlockedUserAddedLiberin;
import org.moera.node.liberin.model.BlockedUserDeletedLiberin;
import org.moera.node.model.notification.BlockingAddedNotification;
import org.moera.node.model.notification.BlockingDeletedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.util.Util;

@LiberinReceptor
public class BlockedUserReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(BlockedUserAddedLiberin liberin) {
        BlockedUser blockedUser = liberin.getBlockedUser();
        if (blockedUser.getBlockedOperation() != BlockedOperation.VISIBILITY
                && blockedUser.getBlockedOperation() != BlockedOperation.INSTANT
                && blockedUser.getEntryNodeName() == null) {
            String postingId = blockedUser.getEntry() != null ? blockedUser.getEntry().getId().toString() : null;
            send(Directions.single(liberin.getNodeId(), blockedUser.getRemoteNodeName()),
                    new BlockingAddedNotification(
                            blockedUser.getBlockedOperation(),
                            postingId,
                            Util.toEpochSecond(blockedUser.getDeadline())));
        }
    }

    @LiberinMapping
    public void deleted(BlockedUserDeletedLiberin liberin) {
        BlockedUser blockedUser = liberin.getBlockedUser();
        if (blockedUser.getBlockedOperation() != BlockedOperation.VISIBILITY
                && blockedUser.getBlockedOperation() != BlockedOperation.INSTANT
                && blockedUser.getEntryNodeName() == null) {
            String postingId = blockedUser.getEntry() != null ? blockedUser.getEntry().getId().toString() : null;
            send(Directions.single(liberin.getNodeId(), blockedUser.getRemoteNodeName()),
                    new BlockingDeletedNotification(blockedUser.getBlockedOperation(), postingId));
        }
    }

}
