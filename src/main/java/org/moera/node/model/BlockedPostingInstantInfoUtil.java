package org.moera.node.model;

import org.moera.lib.node.types.BlockedPostingInstantInfo;
import org.moera.node.data.BlockedInstant;
import org.moera.node.util.Util;

public class BlockedPostingInstantInfoUtil {

    public static BlockedPostingInstantInfo build(BlockedInstant blockedInstant) {
        BlockedPostingInstantInfo info = new BlockedPostingInstantInfo();
        info.setId(blockedInstant.getId().toString());
        info.setStoryType(blockedInstant.getStoryType());
        info.setRemoteOwnerName(blockedInstant.getRemoteOwnerName());
        info.setDeadline(Util.toEpochSecond(blockedInstant.getDeadline()));
        return info;
    }

}
