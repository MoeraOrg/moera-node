package org.moera.node.model.notification;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.util.LogUtil;
import org.moera.node.model.AvatarImageUtil;
import org.springframework.data.util.Pair;

public abstract class ReactionNotification extends Notification {

    @Size(max = 63)
    private String ownerName;

    @Size(max = 96)
    private String ownerFullName;

    @Size(max = 31)
    private String ownerGender;

    @Valid
    private AvatarImage ownerAvatar;

    private boolean negative;

    protected ReactionNotification(NotificationType type) {
        super(type);
    }

    public ReactionNotification(NotificationType type, String ownerName, String ownerFullName, String ownerGender,
                                AvatarImage ownerAvatar, boolean negative) {
        super(type);
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        this.ownerAvatar = ownerAvatar;
        this.negative = negative;
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

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("ownerName", LogUtil.format(ownerName)));
        parameters.add(Pair.of("ownerAvatar", ownerAvatar != null ? AvatarImageUtil.toLogString(ownerAvatar) : "null"));
        parameters.add(Pair.of("negative", LogUtil.format(negative)));
    }

}
