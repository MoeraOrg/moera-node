package org.moera.node.model;

import org.moera.lib.node.types.BlockedUsersChecksums;

public class BlockedUsersChecksumsUtil {

    public static BlockedUsersChecksums build(long visibility) {
        BlockedUsersChecksums blockedUsersChecksums = new BlockedUsersChecksums();
        blockedUsersChecksums.setVisibility(visibility);
        return blockedUsersChecksums;
    }

}
