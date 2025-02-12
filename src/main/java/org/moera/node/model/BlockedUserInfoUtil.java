package org.moera.node.model;

import org.moera.lib.node.types.BlockedUserInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.BlockedUser;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class BlockedUserInfoUtil {

    public static BlockedUserInfo build(BlockedUser blockedUser, Options options) {
        BlockedUserInfo info = new BlockedUserInfo();
        info.setId(blockedUser.getId().toString());
        info.setBlockedOperation(blockedUser.getBlockedOperation());
        info.setNodeName(blockedUser.getRemoteNodeName());
        if (blockedUser.getContact() != null) {
            info.setContact(ContactInfoUtil.build(blockedUser.getContact(), options));
        }
        if (blockedUser.getEntry() != null) {
            info.setEntryId(blockedUser.getEntry().getId().toString());
        }
        info.setEntryNodeName(blockedUser.getEntryNodeName());
        info.setEntryPostingId(blockedUser.getEntryPostingId());
        info.setCreatedAt(Util.toEpochSecond(blockedUser.getCreatedAt()));
        info.setDeadline(Util.toEpochSecond(blockedUser.getDeadline()));
        info.setReasonSrc(blockedUser.getReasonSrc());
        info.setReasonSrcFormat(blockedUser.getReasonSrcFormat());
        info.setReason(blockedUser.getReason());
        return info;
    }

    public static BlockedUserInfo build(BlockedUser blockedUser, Options options, AccessChecker accessChecker) {
        BlockedUserInfo info = build(blockedUser, options);
        protect(info, accessChecker);
        return info;
    }

    public static void protect(BlockedUserInfo blockedUser, AccessChecker accessChecker) {
        ContactInfoUtil.protect(blockedUser.getContact(), accessChecker);
        if (!accessChecker.isPrincipal(Principal.ADMIN, Scope.VIEW_PEOPLE)) {
            blockedUser.setReasonSrc(null);
            blockedUser.setReasonSrcFormat(null);
        }
    }

}
