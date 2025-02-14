package org.moera.node.model;

import org.moera.lib.node.types.BlockedInstantAttributes;
import org.moera.node.data.BlockedInstant;
import org.moera.node.util.Util;

public class BlockedInstantAttributesUtil {

    public static void toBlockedInstant(BlockedInstantAttributes attributes, BlockedInstant blockedInstant) {
        blockedInstant.setStoryType(attributes.getStoryType());
        blockedInstant.setRemoteNodeName(attributes.getRemoteNodeName());
        blockedInstant.setRemotePostingId(attributes.getRemotePostingId());
        blockedInstant.setRemoteOwnerName(attributes.getRemoteOwnerName());
        blockedInstant.setDeadline(Util.toTimestamp(attributes.getDeadline()));
    }

}
