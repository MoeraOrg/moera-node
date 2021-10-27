package org.moera.node.model;

import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.data.BodyFormat;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftType;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SourceFormat;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class DraftText {

    private DraftType draftType;

    @NotBlank
    @Size(max = 63)
    private String receiverName;

    @Size(max = 40)
    private String receiverPostingId;

    @Size(max = 40)
    private String receiverCommentId;

    @Size(max = 96)
    private String ownerFullName;

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

    @Valid
    private AcceptedReactions acceptedReactions;

    private Boolean reactionsVisible;

    private Boolean reactionTotalsVisible;

    private String bodySrc;

    private SourceFormat bodySrcFormat;

    private UUID[] media;

    private Long publishAt;

    @Valid
    private UpdateInfo updateInfo;

    public DraftType getDraftType() {
        return draftType;
    }

    public void setDraftType(DraftType draftType) {
        this.draftType = draftType;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPostingId() {
        return receiverPostingId;
    }

    public void setReceiverPostingId(String receiverPostingId) {
        this.receiverPostingId = receiverPostingId;
    }

    public String getReceiverCommentId() {
        return receiverCommentId;
    }

    public void setReceiverCommentId(String receiverCommentId) {
        this.receiverCommentId = receiverCommentId;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
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

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
    }

    public Boolean getReactionsVisible() {
        return reactionsVisible;
    }

    public void setReactionsVisible(Boolean reactionsVisible) {
        this.reactionsVisible = reactionsVisible;
    }

    public Boolean getReactionTotalsVisible() {
        return reactionTotalsVisible;
    }

    public void setReactionTotalsVisible(Boolean reactionTotalsVisible) {
        this.reactionTotalsVisible = reactionTotalsVisible;
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

    public UUID[] getMedia() {
        return media;
    }

    public void setMedia(UUID[] media) {
        this.media = media;
    }

    public Long getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Long publishAt) {
        this.publishAt = publishAt;
    }

    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }

    public void toDraft(Draft draft, TextConverter textConverter) {
        draft.setDraftType(draftType);
        draft.setEditedAt(Util.now());
        if (ownerFullName != null) {
            draft.setOwnerFullName(ownerFullName);
        }
        if (ownerAvatar != null) {
            if (ownerAvatarMediaFile != null) {
                draft.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (ownerAvatar.getShape() != null) {
                draft.setOwnerAvatarShape(ownerAvatar.getShape());
            }
        }

        if (acceptedReactions != null) {
            if (acceptedReactions.getPositive() != null) {
                draft.setAcceptedReactionsPositive(acceptedReactions.getPositive());
            }
            if (acceptedReactions.getNegative() != null) {
                draft.setAcceptedReactionsNegative(acceptedReactions.getNegative());
            }
        }
        if (reactionsVisible != null) {
            draft.setReactionsVisible(reactionsVisible);
        }
        if (reactionTotalsVisible != null) {
            draft.setReactionTotalsVisible(reactionTotalsVisible);
        }

        if (bodySrcFormat != null) {
            draft.setBodySrcFormat(bodySrcFormat);
        }

        if (!ObjectUtils.isEmpty(bodySrc)) {
            if (draft.getBodySrcFormat() != SourceFormat.APPLICATION) {
                draft.setBodySrc(bodySrc);
                Body body = textConverter.toHtml(draft.getBodySrcFormat(), new Body(bodySrc));
                draft.setBody(body.getEncoded());
                draft.setBodyFormat(BodyFormat.MESSAGE.getValue());
                draft.setHeading(HeadingExtractor.extractHeading(body));
            } else {
                draft.setBodySrc(Body.EMPTY);
                draft.setBody(bodySrc);
                draft.setBodyFormat(BodyFormat.APPLICATION.getValue());
            }
        }

        if (publishAt != null) {
            draft.setPublishAt(Util.toTimestamp(publishAt));
        }

        if (updateInfo != null) {
            if (updateInfo.getImportant() != null) {
                draft.setUpdateImportant(updateInfo.getImportant());
            }
            if (updateInfo.getDescription() != null) {
                draft.setUpdateDescription(updateInfo.getDescription());
            }
        }
    }

}
