package org.moera.node.model;

import org.moera.lib.node.types.BlockedByUserInfo;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.BlockedByUser;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class BlockedByUserInfoUtil {

    public static BlockedByUserInfo build(BlockedByUser blockedByUser, Options options) {
        BlockedByUserInfo info = new BlockedByUserInfo();
        info.setId(blockedByUser.getId().toString());
        info.setBlockedOperation(blockedByUser.getBlockedOperation());
        if (blockedByUser.getContact() != null) {
            info.setContact(ContactInfoUtil.build(blockedByUser.getContact(), options));
        }
        info.setNodeName(blockedByUser.getRemoteNodeName());
        info.setPostingId(blockedByUser.getRemotePostingId());
        info.setCreatedAt(Util.toEpochSecond(blockedByUser.getCreatedAt()));
        info.setDeadline(Util.toEpochSecond(blockedByUser.getDeadline()));
        info.setReason(blockedByUser.getReason());
        return info;
    }

    public static BlockedByUserInfo build(BlockedByUser blockedByUser, Options options, AccessChecker accessChecker) {
        BlockedByUserInfo info = build(blockedByUser, options);
        protect(info, accessChecker);
        return info;
    }

    public static void protect(BlockedByUserInfo info, AccessChecker accessChecker) {
        ContactInfoUtil.protect(info.getContact(), accessChecker);
    }

}
