package org.moera.node.model;

import org.moera.lib.node.types.BlockedByUserInfo;
import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.BlockedByUser;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class BlockedByUserInfoUtil {

    public static BlockedByUserInfo build(BlockedByUser blockedByUser, Options options) {
        ContactInfo contactInfo = blockedByUser.getContact() != null
            ? ContactInfoUtil.build(blockedByUser.getContact(), options)
            : null;
        return build(blockedByUser, contactInfo);
    }

    public static BlockedByUserInfo build(BlockedByUser blockedByUser, ContactInfo contactInfo) {
        BlockedByUserInfo info = new BlockedByUserInfo();
        info.setId(blockedByUser.getId().toString());
        info.setBlockedOperation(blockedByUser.getBlockedOperation());
        info.setContact(contactInfo);
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

    public static BlockedByUserInfo build(
        BlockedByUser blockedByUser, ContactInfo contactInfo, AccessChecker accessChecker
    ) {
        BlockedByUserInfo info = build(blockedByUser, contactInfo);
        protect(info, accessChecker);
        return info;
    }

    public static void protect(BlockedByUserInfo info, AccessChecker accessChecker) {
        if (info.getContact() != null) {
            ContactInfoUtil.protect(info.getContact(), accessChecker);
        }
    }

}
