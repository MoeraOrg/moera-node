package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class CommentReactionAddedNotification extends CommentReactionNotification {

    private String postingHeading;
    private String commentHeading;
    private int emoji;

    public CommentReactionAddedNotification() {
        super(NotificationType.COMMENT_REACTION_ADDED);
    }

    public CommentReactionAddedNotification(UUID postingId, UUID commentId, String postingHeading,
                                            String commentHeading, String ownerName, String ownerFullName,
                                            AvatarImage ownerAvatar, boolean negative, int emoji) {
        super(NotificationType.COMMENT_REACTION_ADDED, postingId, commentId, ownerName, ownerFullName, ownerAvatar,
                negative);
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
        this.emoji = emoji;
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
        parameters.add(Pair.of("emoji", LogUtil.format(emoji)));
    }

}
