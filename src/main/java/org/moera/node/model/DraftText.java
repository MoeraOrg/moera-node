package org.moera.node.model;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.AcceptedReactions;
import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.RemoteMedia;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.UpdateInfo;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Draft;
import org.moera.node.data.MediaFile;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.MediaExtractor;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

public class DraftText {

    private static final Logger log = LoggerFactory.getLogger(DraftText.class);

    private DraftType draftType;

    @NotBlank
    @Size(max = 63)
    private String receiverName;

    @Size(max = 40)
    private String receiverPostingId;

    @Size(max = 40)
    private String receiverCommentId;

    @Size(max = 40)
    private String repliedToId;

    @Size(max = 96)
    private String ownerFullName;

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

    @Valid
    private AcceptedReactions acceptedReactions;

    private String bodySrc;

    private SourceFormat bodySrcFormat;

    private RemoteMedia[] media;

    private Long publishAt;

    @Valid
    private UpdateInfo updateInfo;

    private Map<String, Principal> operations;

    private Map<String, Principal> commentOperations;

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

    public String getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(String repliedToId) {
        this.repliedToId = repliedToId;
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

    public RemoteMedia[] getMedia() {
        return media;
    }

    public void setMedia(RemoteMedia[] media) {
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

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Map<String, Principal> getCommentOperations() {
        return commentOperations;
    }

    public void setCommentOperations(Map<String, Principal> commentOperations) {
        this.commentOperations = commentOperations;
    }

    public void toDraft(Draft draft, TextConverter textConverter) {
        draft.setDraftType(draftType);
        draft.setEditedAt(Util.now());
        draft.setRepliedToId(repliedToId);
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
        if (bodySrcFormat != null) {
            draft.setBodySrcFormat(bodySrcFormat);
        }

        if (!ObjectUtils.isEmpty(bodySrc)) {
            if (draft.getBodySrcFormat() != SourceFormat.APPLICATION) {
                draft.setBodySrc(bodySrc);
                Body body = textConverter.toHtml(draft.getBodySrcFormat(), new Body(bodySrc));
                draft.setBody(body.getEncoded());
                draft.setBodyFormat(BodyFormat.MESSAGE.getValue());
                draft.setHeading(HeadingExtractor.extractHeading(body, hasAttachedGallery(body, media), true));
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

        if (operations != null) {
            try {
                draft.setOperations(new ObjectMapper().writeValueAsString(operations));
            } catch (JsonProcessingException e) {
                log.error("Error serializing DraftText.operations", e);
            }
        }

        if (commentOperations != null) {
            try {
                draft.setChildOperations(new ObjectMapper().writeValueAsString(commentOperations));
            } catch (JsonProcessingException e) {
                log.error("Error serializing DraftText.commentOperations", e);
            }
        }
    }

    private static boolean hasAttachedGallery(Body body, RemoteMedia[] media) {
        if (ObjectUtils.isEmpty(media)) {
            return false;
        }
        int embeddedCount = MediaExtractor.extractMediaFileIds(body).size();
        return media.length > embeddedCount;
    }

}
