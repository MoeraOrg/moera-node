package org.moera.node.model;

import org.moera.lib.node.types.BlockedInstantInfo;
import org.moera.node.data.BlockedInstant;
import org.moera.node.util.Util;

public class BlockedInstantInfoUtil {

    public static BlockedInstantInfo build(BlockedInstant blockedInstant) {
        BlockedInstantInfo info = new BlockedInstantInfo();
        info.setId(blockedInstant.getId().toString());
        info.setStoryType(blockedInstant.getStoryType());
        if (blockedInstant.getEntry() != null) {
            info.setEntryId(blockedInstant.getEntry().getId().toString());
        }
        info.setRemoteNodeName(blockedInstant.getRemoteNodeName());
        info.setRemotePostingId(blockedInstant.getRemotePostingId());
        info.setRemoteOwnerName(blockedInstant.getRemoteOwnerName());
        info.setCreatedAt(Util.toEpochSecond(blockedInstant.getCreatedAt()));
        info.setDeadline(Util.toEpochSecond(blockedInstant.getDeadline()));
        return info;
    }

}
