package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class PostingReactionAddedNotification extends PostingReactionNotification {

    private String parentHeading;
    private String postingHeading;
    private int emoji;

    public PostingReactionAddedNotification() {
        super(NotificationType.POSTING_REACTION_ADDED);
    }

    public PostingReactionAddedNotification(UUID parentPostingId, UUID parentCommentId, UUID parentMediaId,
                                            String parentHeading, UUID postingId, String postingHeading,
                                            String ownerName, String ownerFullName, AvatarImage ownerAvatar,
                                            boolean negative, int emoji) {
        super(NotificationType.POSTING_REACTION_ADDED, parentPostingId, parentCommentId, parentMediaId, postingId,
              ownerName, ownerFullName, ownerAvatar, negative);
        this.parentHeading = parentHeading;
        this.postingHeading = postingHeading;
        this.emoji = emoji;
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
        parameters.add(Pair.of("emoji", LogUtil.format(emoji)));
    }

}
