package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.SheriffMark;
import org.moera.node.model.AvatarImage;
import org.moera.node.util.SheriffUtil;
import org.springframework.data.util.Pair;

public class MentionPostingAddedNotification extends MentionPostingNotification {

    @Size(max = 63)
    private String ownerName;

    @Size(max = 96)
    private String ownerFullName;

    @Size(max = 31)
    private String ownerGender;

    @Valid
    private AvatarImage ownerAvatar;

    @Size(max = 255)
    private String heading;

    @Size(max = 4096)
    private List<String> sheriffs;

    @Size(max = 4096)
    private List<SheriffMark> sheriffMarks;

    public MentionPostingAddedNotification() {
        super(NotificationType.MENTION_POSTING_ADDED);
    }

    public MentionPostingAddedNotification(UUID postingId, String ownerName, String ownerFullName, String ownerGender,
                                           AvatarImage ownerAvatar, String heading, List<String> sheriffs,
                                           List<SheriffMark> sheriffMarks) {
        super(NotificationType.MENTION_POSTING_ADDED, postingId);
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        this.ownerAvatar = ownerAvatar;
        this.heading = heading;
        this.sheriffs = sheriffs;
        this.sheriffMarks = sheriffMarks;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
    }

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public List<String> getSheriffs() {
        return sheriffs;
    }

    public void setSheriffs(List<String> sheriffs) {
        this.sheriffs = sheriffs;
    }

    public List<SheriffMark> getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(List<SheriffMark> sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("ownerName", LogUtil.format(ownerName)));
        parameters.add(Pair.of("heading", LogUtil.format(heading)));
        parameters.add(Pair.of("sheriffs", LogUtil.format(SheriffUtil.serializeSheriffs(sheriffs).orElse(null))));
        parameters.add(Pair.of("sheriffMarks",
                LogUtil.format(SheriffUtil.serializeSheriffMarks(sheriffMarks).orElse(null))));
    }

}
