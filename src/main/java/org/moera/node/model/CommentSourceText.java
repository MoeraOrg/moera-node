package org.moera.node.model;

import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SourceFormat;

public class CommentSourceText {

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

    @Size(max = 65535)
    @NotBlank
    private String bodySrc;

    private SourceFormat bodySrcFormat;

    @Valid
    private MediaWithDigest[] media;

    @Valid
    private AcceptedReactions acceptedReactions;

    private UUID repliedToId;

    private Map<String, Principal> operations;

    private Map<String, Principal> reactionOperations;

    private Map<String, Principal> seniorOperations;

    public CommentSourceText() {
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

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public MediaWithDigest[] getMedia() {
        return media;
    }

    public void setMedia(MediaWithDigest[] media) {
        this.media = media;
    }

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
    }

    public UUID getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(UUID repliedToId) {
        this.repliedToId = repliedToId;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Map<String, Principal> getReactionOperations() {
        return reactionOperations;
    }

    public void setReactionOperations(Map<String, Principal> reactionOperations) {
        this.reactionOperations = reactionOperations;
    }

    public Map<String, Principal> getSeniorOperations() {
        return seniorOperations;
    }

    public void setSeniorOperations(Map<String, Principal> seniorOperations) {
        this.seniorOperations = seniorOperations;
    }

}
