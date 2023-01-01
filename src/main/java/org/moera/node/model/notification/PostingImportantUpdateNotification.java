package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class PostingImportantUpdateNotification extends PostingSubscriberNotification {

    @Size(max = 63)
    private String postingOwnerName;

    @Size(max = 96)
    private String postingOwnerFullName;

    @Size(max = 31)
    private String postingOwnerGender;

    @Valid
    private AvatarImage postingOwnerAvatar;

    @Size(max = 255)
    private String postingHeading;

    @Size(max = 255)
    private String description;

    public PostingImportantUpdateNotification() {
        super(NotificationType.POSTING_IMPORTANT_UPDATE);
    }

    public PostingImportantUpdateNotification(String postingOwnerName, String postingOwnerFullName,
                                              String postingOwnerGender, AvatarImage postingOwnerAvatar, UUID postingId,
                                              String postingHeading, String description) {
        super(NotificationType.POSTING_IMPORTANT_UPDATE, postingId.toString());
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerGender = postingOwnerGender;
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

    public String getPostingOwnerGender() {
        return postingOwnerGender;
    }

    public void setPostingOwnerGender(String postingOwnerGender) {
        this.postingOwnerGender = postingOwnerGender;
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
