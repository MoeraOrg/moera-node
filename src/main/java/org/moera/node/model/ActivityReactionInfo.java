package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.OwnReaction;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityReactionInfo {

    private String remoteNodeName;
    private String remoteFullName;
    private AvatarImage remoteAvatar;
    private String remotePostingId;
    private boolean negative;
    private int emoji;
    private long createdAt;

    public ActivityReactionInfo() {
    }

    public ActivityReactionInfo(OwnReaction reaction) {
        remoteNodeName = reaction.getRemoteNodeName();
        remoteFullName = reaction.getRemoteFullName();
        if (reaction.getRemoteAvatarMediaFile() != null) {
            remoteAvatar = new AvatarImage(reaction.getRemoteAvatarMediaFile(), reaction.getRemoteAvatarShape());
        }
        remotePostingId = reaction.getRemotePostingId();
        negative = reaction.isNegative();
        emoji = reaction.getEmoji();
        createdAt = Util.toEpochSecond(reaction.getCreatedAt());
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public AvatarImage getRemoteAvatar() {
        return remoteAvatar;
    }

    public void setRemoteAvatar(AvatarImage remoteAvatar) {
        this.remoteAvatar = remoteAvatar;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
