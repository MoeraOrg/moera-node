package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.ReactionAttributes;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Reaction;

public class ReactionDescription {

    private String ownerName;

    private String ownerFullName;

    private String ownerGender;

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

    private boolean negative;

    private int emoji;

    private byte[] signature;

    private short signatureVersion;

    private Map<String, Principal> operations;

    public ReactionDescription() {
    }

    public ReactionDescription(String ownerName, String ownerFullName, String ownerGender, Avatar ownerAvatar,
                               ReactionAttributes attributes) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        this.ownerAvatar = ownerAvatar != null ? AvatarDescriptionUtil.build(ownerAvatar) : null;
        negative = attributes.isNegative();
        emoji = attributes.getEmoji();
        operations = new HashMap<>();
        operations.put("view", attributes.getOperations().getView());
        operations.put("delete", attributes.getOperations().getDelete());
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

    public AvatarDescription getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarDescription ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public MediaFile getOwnerAvatarMediaFile() {
        return ownerAvatarMediaFile;
    }

    public void setOwnerAvatarMediaFile(MediaFile ownerAvatarMediaFile) {
        this.ownerAvatarMediaFile = ownerAvatarMediaFile;
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

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public void toReaction(Reaction reaction) {
        reaction.setOwnerName(ownerName);
        reaction.setOwnerFullName(ownerFullName);
        reaction.setOwnerGender(ownerGender);
        if (ownerAvatar != null) {
            if (ownerAvatarMediaFile != null) {
                reaction.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (ownerAvatar.getShape() != null) {
                reaction.setOwnerAvatarShape(ownerAvatar.getShape());
            }
        }
        reaction.setNegative(negative);
        reaction.setEmoji(emoji);
        reaction.setSignature(signature);
        reaction.setSignatureVersion(signatureVersion);
        if (getPrincipal("view") != null) {
            reaction.setViewPrincipal(getPrincipal("view"));
        }
    }

}
