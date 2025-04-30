package org.moera.node.liberin.receptor;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.node.data.BlockedUser;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.BlockedUserAddedLiberin;
import org.moera.node.liberin.model.BlockedUserDeletedLiberin;
import org.moera.node.model.BlockedUserInfoUtil;
import org.moera.node.model.event.BlockedUserAddedEvent;
import org.moera.node.model.event.BlockedUserDeletedEvent;
import org.moera.node.model.notification.BlockingAddedNotificationUtil;
import org.moera.node.model.notification.BlockingDeletedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;
import org.moera.node.util.Util;

@LiberinReceptor
public class BlockedUserReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(BlockedUserAddedLiberin liberin) {
        BlockedUser blockedUser = liberin.getBlockedUser();
        send(liberin, new BlockedUserAddedEvent(
            BlockedUserInfoUtil.build(blockedUser, universalContext.getOptions()),
            BlockedUser.getViewAllE(universalContext.getOptions())
        ));
        if (
            blockedUser.getBlockedOperation() != BlockedOperation.INSTANT
            && blockedUser.getEntryNodeName() == null
            && blockedUser.getDeadline() == null
        ) {
            send(
                Directions.searchSubscribers(
                    liberin.getNodeId(), BlockedUser.getViewAllE(universalContext.getOptions())
                ),
                SearchContentUpdatedNotificationUtil.buildBlockUpdate(
                    SearchContentUpdateType.BLOCK, blockedUser.getRemoteNodeName(), blockedUser.getBlockedOperation()
                )
            );
        }
        if (
            blockedUser.getBlockedOperation() != BlockedOperation.VISIBILITY
            && blockedUser.getBlockedOperation() != BlockedOperation.INSTANT
            && blockedUser.getEntryNodeName() == null
        ) {
            String postingId = null;
            String postingHeading = null;
            if (blockedUser.getEntry() != null) {
                postingId = blockedUser.getEntry().getId().toString();
                postingHeading = blockedUser.getEntry().getCurrentRevision().getHeading();
            }
            send(
                Directions.single(liberin.getNodeId(), blockedUser.getRemoteNodeName()),
                BlockingAddedNotificationUtil.build(
                    blockedUser.getBlockedOperation(),
                    postingId,
                    postingHeading,
                    Util.toEpochSecond(blockedUser.getDeadline()),
                    blockedUser.getReason()
                )
            );
        }
    }

    @LiberinMapping
    public void deleted(BlockedUserDeletedLiberin liberin) {
        BlockedUser blockedUser = liberin.getBlockedUser();
        if (
            blockedUser.getBlockedOperation() != BlockedOperation.INSTANT
            && blockedUser.getEntryNodeName() == null
            && blockedUser.getDeadline() == null
        ) {
            send(
                Directions.searchSubscribers(
                    liberin.getNodeId(), BlockedUser.getViewAllE(universalContext.getOptions())
                ),
                SearchContentUpdatedNotificationUtil.buildBlockUpdate(
                    SearchContentUpdateType.UNBLOCK, blockedUser.getRemoteNodeName(), blockedUser.getBlockedOperation()
                )
            );
        }
        send(liberin, new BlockedUserDeletedEvent(
            BlockedUserInfoUtil.build(blockedUser, universalContext.getOptions()),
            BlockedUser.getViewAllE(universalContext.getOptions())
        ));
        if (
            blockedUser.getBlockedOperation() != BlockedOperation.VISIBILITY
            && blockedUser.getBlockedOperation() != BlockedOperation.INSTANT
            && blockedUser.getEntryNodeName() == null
        ) {
            String postingId = null;
            String postingHeading = null;
            if (blockedUser.getEntry() != null) {
                postingId = blockedUser.getEntry().getId().toString();
                postingHeading = blockedUser.getEntry().getCurrentRevision().getHeading();
            }
            send(
                Directions.single(liberin.getNodeId(), blockedUser.getRemoteNodeName()),
                BlockingDeletedNotificationUtil.build(blockedUser.getBlockedOperation(), postingId, postingHeading)
            );
        }
    }

}
