package org.moera.node.model;

import org.moera.lib.node.types.ActivityReactionInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.OwnReaction;
import org.moera.node.util.Util;

public class ActivityReactionInfoUtil {

    public static ActivityReactionInfo build(OwnReaction reaction, DirectServeConfig config) {
        ActivityReactionInfo info = new ActivityReactionInfo();
        info.setRemoteNodeName(reaction.getRemoteNodeName());
        info.setRemoteFullName(reaction.getRemoteFullName());
        if (reaction.getRemoteAvatarMediaFile() != null) {
            info.setRemoteAvatar(
                AvatarImageUtil.build(reaction.getRemoteAvatarMediaFile(), reaction.getRemoteAvatarShape(), config)
            );
        }
        info.setRemotePostingId(reaction.getRemotePostingId());
        info.setNegative(reaction.isNegative());
        info.setEmoji(reaction.getEmoji());
        info.setCreatedAt(Util.toEpochSecond(reaction.getCreatedAt()));
        return info;
    }

}
