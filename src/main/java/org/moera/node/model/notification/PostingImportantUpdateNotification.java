package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class PostingImportantUpdateNotification extends PostingSubscriberNotification {

    private String postingOwnerName;
    private String postingOwnerFullName;
    private AvatarImage postingOwnerAvatar;
    private String postingHeading;
    private String description;

    public PostingImportantUpdateNotification() {
        super(NotificationType.POSTING_IMPORTANT_UPDATE);
    }

    public PostingImportantUpdateNotification(String postingOwnerName, String postingOwnerFullName,
                                              AvatarImage postingOwnerAvatar, UUID postingId, String postingHeading,
                                              String description) {
        super(NotificationType.POSTING_IMPORTANT_UPDATE, postingId.toString());
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingHeading = postingHeading;
        this.description = description;
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public String getPostingOwnerFullName() {
        return postingOwnerFullName;
    }

    public void setPostingOwnerFullName(String postingOwnerFullName) {
        this.postingOwnerFullName = postingOwnerFullName;
    }

    public AvatarImage getPostingOwnerAvatar() {
        return postingOwnerAvatar;
    }

    public void setPostingOwnerAvatar(AvatarImage postingOwnerAvatar) {
        this.postingOwnerAvatar = postingOwnerAvatar;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("postingOwnerName", LogUtil.format(postingOwnerName)));
        parameters.add(Pair.of("postingHeading", LogUtil.format(postingHeading)));
        parameters.add(Pair.of("description", LogUtil.format(description)));
    }

}
