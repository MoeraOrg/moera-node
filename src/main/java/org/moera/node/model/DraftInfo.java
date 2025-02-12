package org.moera.node.model;

import java.util.Comparator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Draft;
import org.moera.node.data.EntryAttachment;
import org.moera.node.model.body.Body;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftInfo {

    private static final Logger log = LoggerFactory.getLogger(DraftInfo.class);

    private String id;
    private DraftType draftType;
    private String receiverName;
    private String receiverPostingId;
    private String receiverCommentId;
    private String repliedToId;
    private Long createdAt;
    private Long editedAt;
    private Long deadline;
    private String ownerFullName;
    private AvatarImage ownerAvatar;
    private AcceptedReactions acceptedReactions;
    private Body bodySrc;
    private SourceFormat bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private MediaAttachment[] media;
    private String heading;
    private Long publishAt;
    private UpdateInfo updateInfo;
    private Map<String, Principal> operations;
    private Map<String, Principal> commentOperations;

    public DraftInfo() {
    }

    public DraftInfo(Draft draft) {
        id = draft.getId().toString();
        draftType = draft.getDraftType();
        receiverName = draft.getReceiverName();
        receiverPostingId = draft.getReceiverPostingId();
        receiverCommentId = draft.getReceiverCommentId();
        repliedToId = draft.getRepliedToId();
        createdAt = Util.toEpochSecond(draft.getCreatedAt());
        editedAt = Util.toEpochSecond(draft.getEditedAt());
        deadline = Util.toEpochSecond(draft.getDeadline());
        ownerFullName = draft.getOwnerFullName();
        if (draft.getOwnerAvatarMediaFile() != null) {
            ownerAvatar = AvatarImageUtil.build(draft.getOwnerAvatarMediaFile(), draft.getOwnerAvatarShape());
        }
        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(draft.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(draft.getAcceptedReactionsNegative());
        bodySrc = new Body(draft.getBodySrc());
        bodySrcFormat = draft.getBodySrcFormat();
        body = new Body(draft.getBody());
        bodyFormat = draft.getBodyFormat();
        media = draft.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> new MediaAttachment(ea, null))
                .toArray(MediaAttachment[]::new);
        heading = draft.getHeading();
        publishAt = Util.toEpochSecond(draft.getPublishAt());
        if (!UpdateInfo.isEmpty(draft)) {
            updateInfo = new UpdateInfo(draft);
        }
        try {
            operations = new ObjectMapper().readValue(draft.getOperations(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing Draft.operations", e);
        }
        try {
            commentOperations = new ObjectMapper().readValue(draft.getChildOperations(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing Draft.childOperations", e);
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

    public String getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(String repliedToId) {
        this.repliedToId = repliedToId;
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

}
