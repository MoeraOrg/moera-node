package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.moera.lib.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class PostingReactionAddedNotification extends PostingReactionNotification {

    @Size(max = 63)
    private String parentPostingNodeName;

    @Size(max = 96)
    private String parentPostingFullName;

    @Size(max = 31)
    private String parentPostingGender;

    @Valid
    private AvatarImage parentPostingAvatar;

    @Size(max = 255)
    private String parentHeading;

    @Size(max = 255)
    private String postingHeading;

    private int emoji;

    public PostingReactionAddedNotification() {
        super(NotificationType.POSTING_REACTION_ADDED);
    }

    public PostingReactionAddedNotification(String parentPostingNodeName, String parentPostingFullName,
                                            String parentPostingGender, AvatarImage parentPostingAvatar,
                                            UUID parentPostingId, UUID parentCommentId, UUID parentMediaId,
                                            String parentHeading, UUID postingId, String postingHeading,
                                            String ownerName, String ownerFullName, String ownerGender,
                                            AvatarImage ownerAvatar, boolean negative, int emoji) {
        super(NotificationType.POSTING_REACTION_ADDED, parentPostingId, parentCommentId, parentMediaId, postingId,
              ownerName, ownerFullName, ownerGender, ownerAvatar, negative);
        this.parentPostingNodeName = parentPostingNodeName;
        this.parentPostingFullName = parentPostingFullName;
        this.parentPostingGender = parentPostingGender;
        this.parentPostingAvatar = parentPostingAvatar;
        this.parentHeading = parentHeading;
        this.postingHeading = postingHeading;
        this.emoji = emoji;
    }

    public String getParentPostingNodeName() {
        return parentPostingNodeName;
    }

    public void setParentPostingNodeName(String parentPostingNodeName) {
        this.parentPostingNodeName = parentPostingNodeName;
    }

    public String getParentPostingFullName() {
        return parentPostingFullName;
    }

    public void setParentPostingFullName(String parentPostingFullName) {
        this.parentPostingFullName = parentPostingFullName;
    }

    public String getParentPostingGender() {
        return parentPostingGender;
    }

    public void setParentPostingGender(String parentPostingGender) {
        this.parentPostingGender = parentPostingGender;
    }

    public AvatarImage getParentPostingAvatar() {
        return parentPostingAvatar;
    }

    public void setParentPostingAvatar(AvatarImage parentPostingAvatar) {
        this.parentPostingAvatar = parentPostingAvatar;
    }

    public String getParentHeading() {
        return parentHeading;
    }

    public void setParentHeading(String parentHeading) {
        this.parentHeading = parentHeading;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("parentPostingNodeName", LogUtil.format(parentPostingNodeName)));
        parameters.add(Pair.of("parentPostingFullName", LogUtil.format(parentPostingFullName)));
        parameters.add(Pair.of("parentHeading", LogUtil.format(parentHeading)));
        parameters.add(Pair.of("postingHeading", LogUtil.format(postingHeading)));
        parameters.add(Pair.of("emoji", LogUtil.format(emoji)));
    }

}
