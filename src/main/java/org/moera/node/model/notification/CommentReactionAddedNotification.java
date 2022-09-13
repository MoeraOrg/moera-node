package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class CommentReactionAddedNotification extends CommentReactionNotification {

    private String postingNodeName;
    private String postingFullName;
    private AvatarImage postingAvatar;
    private String postingHeading;
    private String commentHeading;
    private int emoji;

    public CommentReactionAddedNotification() {
        super(NotificationType.COMMENT_REACTION_ADDED);
    }

    public CommentReactionAddedNotification(String postingNodeName, String postingFullName, AvatarImage postingAvatar,
                                            UUID postingId, UUID commentId, String postingHeading,
                                            String commentHeading, String ownerName, String ownerFullName,
                                            AvatarImage ownerAvatar, boolean negative, int emoji) {
        super(NotificationType.COMMENT_REACTION_ADDED, postingId, commentId, ownerName, ownerFullName, ownerAvatar,
                negative);
        this.postingNodeName = postingNodeName;
        this.postingFullName = postingFullName;
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
