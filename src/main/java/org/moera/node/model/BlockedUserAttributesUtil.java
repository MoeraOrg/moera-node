package org.moera.node.model;

import org.moera.lib.node.types.BlockedUserAttributes;
import org.moera.lib.node.types.SourceFormat;
import org.moera.node.data.BlockedUser;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class BlockedUserAttributesUtil {

    public static void toBlockedUser(
        BlockedUserAttributes attributes, BlockedUser blockedUser, TextConverter textConverter
    ) {
        blockedUser.setBlockedOperation(attributes.getBlockedOperation());
        blockedUser.setRemoteNodeName(attributes.getNodeName());
        blockedUser.setEntryNodeName(attributes.getEntryNodeName());
        blockedUser.setEntryPostingId(attributes.getEntryPostingId());
        blockedUser.setDeadline(Util.toTimestamp(attributes.getDeadline()));
        blockedUser.setReasonSrc(attributes.getReasonSrc() != null ? attributes.getReasonSrc() : "");
        blockedUser.setReasonSrcFormat(
            attributes.getReasonSrcFormat() != null ? attributes.getReasonSrcFormat() : SourceFormat.MARKDOWN
        );
        if (
            !ObjectUtils.isEmpty(attributes.getReasonSrc())
            && attributes.getReasonSrcFormat() != SourceFormat.APPLICATION
        ) {
            blockedUser.setReason(textConverter.toHtml(attributes.getReasonSrcFormat(), attributes.getReasonSrc()));
        } else {
            blockedUser.setReason(blockedUser.getReasonSrc());
        }
    }

}
