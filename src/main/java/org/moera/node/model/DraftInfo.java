package org.moera.node.model;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftType;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.SourceFormat;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftInfo {

    private String id;
    private DraftType draftType;
    private String receiverName;
    private String receiverPostingId;
    private String receiverCommentId;
    private Long createdAt;
    private Long editedAt;
    private Long deadline;
    private String ownerFullName;
    private AvatarImage ownerAvatar;
    private AcceptedReactions acceptedReactions;
    private Boolean reactionsVisible;
    private Boolean reactionTotalsVisible;
    private Body bodySrc;
    private SourceFormat bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private MediaAttachment[] media;
    private String heading;
    private Long publishAt;
    private UpdateInfo updateInfo;

    public DraftInfo() {
    }

    public DraftInfo(Draft draft) {
        id = draft.getId().toString();
        draftType = draft.getDraftType();
        receiverName = draft.getReceiverName();
        receiverPostingId = draft.getReceiverPostingId();
        receiverCommentId = draft.getReceiverCommentId();
        createdAt = Util.toEpochSecond(draft.getCreatedAt());
        editedAt = Util.toEpochSecond(draft.getEditedAt());
        deadline = Util.toEpochSecond(draft.getDeadline());
        ownerFullName = draft.getOwnerFullName();
        if (draft.getOwnerAvatarMediaFile() != null) {
            ownerAvatar = new AvatarImage(draft.getOwnerAvatarMediaFile(), draft.getOwnerAvatarShape());
        }
        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(draft.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(draft.getAcceptedReactionsNegative());
        reactionsVisible = draft.isReactionsVisible();
        reactionTotalsVisible = draft.isReactionTotalsVisible();
        bodySrc = new Body(draft.getBodySrc());
        bodySrcFormat = draft.getBodySrcFormat();
        body = new Body(draft.getBody());
        bodyFormat = draft.getBodyFormat();
        media = draft.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(MediaAttachment::new)
                .toArray(MediaAttachment[]::new);
        heading = draft.getHeading();
        publishAt = Util.toEpochSecond(draft.getPublishAt());
        if (!UpdateInfo.isEmpty(draft)) {
            updateInfo = new UpdateInfo(draft);
        }

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Long editedAt) {
        this.editedAt = editedAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
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

    public Body getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(Body bodySrc) {
        this.bodySrc = bodySrc;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public MediaAttachment[] getMedia() {
        return media;
    }

    public void setMedia(MediaAttachment[] media) {
        this.media = media;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
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

}
