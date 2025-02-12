package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class CommentReactionAddedNotification extends CommentReactionNotification {

    @Size(max = 63)
    private String postingNodeName;

    @Size(max = 96)
    private String postingFullName;

    @Size(max = 31)
    private String postingGender;

    @Valid
    private AvatarImage postingAvatar;

    @Size(max = 255)
    private String postingHeading;

    @Size(max = 255)
    private String commentHeading;

    private int emoji;

    public CommentReactionAddedNotification() {
        super(NotificationType.COMMENT_REACTION_ADDED);
    }

    public CommentReactionAddedNotification(String postingNodeName, String postingFullName, String postingGender,
                                            AvatarImage postingAvatar, UUID postingId, UUID commentId,
                                            String postingHeading, String commentHeading, String ownerName,
                                            String ownerFullName, String ownerGender, AvatarImage ownerAvatar,
                                            boolean negative, int emoji) {
        super(NotificationType.COMMENT_REACTION_ADDED, postingId, commentId, ownerName, ownerFullName, ownerGender,
                ownerAvatar, negative);
        this.postingNodeName = postingNodeName;
        this.postingFullName = postingFullName;
        this.postingGender = postingGender;
        this.postingAvatar = postingAvatar;
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
        this.emoji = emoji;
    }

    public String getPostingNodeName() {
        return postingNodeName;
    }

    public void setPostingNodeName(String postingNodeName) {
        this.postingNodeName = postingNodeName;
    }

    public String getPostingFullName() {
        return postingFullName;
    }

    public void setPostingFullName(String postingFullName) {
        this.postingFullName = postingFullName;
    }

    public String getPostingGender() {
        return postingGender;
    }

    public void setPostingGender(String postingGender) {
        this.postingGender = postingGender;
    }

    public AvatarImage getPostingAvatar() {
        return postingAvatar;
    }

    public void setPostingAvatar(AvatarImage postingAvatar) {
        this.postingAvatar = postingAvatar;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
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
        parameters.add(Pair.of("postingNodeName", LogUtil.format(postingNodeName)));
        parameters.add(Pair.of("postingFullName", LogUtil.format(postingFullName)));
        parameters.add(Pair.of("emoji", LogUtil.format(emoji)));
    }

}
