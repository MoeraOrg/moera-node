package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.SourceFormat;
import org.moera.node.util.Util;

@Entity
@Table(name = "drafts")
public class Draft {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private DraftType draftType;

    @NotNull
    @Size(max = 63)
    private String receiverName;

    @Size(max = 40)
    private String receiverPostingId;

    @Size(max = 40)
    private String receiverCommentId;

    @Size(max = 40)
    private String repliedToId;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp editedAt = Util.now();

    private Timestamp deadline;

    @Size(max = 96)
    private String ownerFullName;

    @ManyToOne
    private MediaFile ownerAvatarMediaFile;

    @Size(max = 8)
    private String ownerAvatarShape;

    @NotNull
    @Size(max = 255)
    private String rejectedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String rejectedReactionsNegative = "*";

    @NotNull
    @Size(max = 255)
    private String childRejectedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String childRejectedReactionsNegative = "";

    @NotNull
    private String bodySrc = "";

    @NotNull
    @Enumerated
    private SourceFormat bodySrcFormat = SourceFormat.PLAIN_TEXT;

    @NotNull
    private String body = "";

    @NotNull
    @Size(max = 75)
    private String bodyFormat = BodyFormat.MESSAGE.getValue();

    @NotNull
    private String heading = "";

    private Timestamp publishAt;

    @NotNull
    private boolean updateImportant;

    @NotNull
    private String updateDescription = "";

    @NotNull
    private String operations = "{}";

    @NotNull
    private String childOperations = "{}";

    @NotNull
    private boolean allowAnonymousChildren;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "draft")
    private Set<EntryAttachment> attachments = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Timestamp editedAt) {
        this.editedAt = editedAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public MediaFile getOwnerAvatarMediaFile() {
        return ownerAvatarMediaFile;
    }

    public void setOwnerAvatarMediaFile(MediaFile ownerAvatarMediaFile) {
        this.ownerAvatarMediaFile = ownerAvatarMediaFile;
    }

    public String getOwnerAvatarShape() {
        return ownerAvatarShape;
    }

    public void setOwnerAvatarShape(String ownerAvatarShape) {
        this.ownerAvatarShape = ownerAvatarShape;
    }

    public String getRejectedReactionsPositive() {
        return rejectedReactionsPositive;
    }

    public void setRejectedReactionsPositive(String rejectedReactionsPositive) {
        this.rejectedReactionsPositive = rejectedReactionsPositive;
    }

    public String getRejectedReactionsNegative() {
        return rejectedReactionsNegative;
    }

    public void setRejectedReactionsNegative(String rejectedReactionsNegative) {
        this.rejectedReactionsNegative = rejectedReactionsNegative;
    }

    public String getChildRejectedReactionsPositive() {
        return childRejectedReactionsPositive;
    }

    public void setChildRejectedReactionsPositive(String childRejectedReactionsPositive) {
        this.childRejectedReactionsPositive = childRejectedReactionsPositive;
    }

    public String getChildRejectedReactionsNegative() {
        return childRejectedReactionsNegative;
    }

    public void setChildRejectedReactionsNegative(String childRejectedReactionsNegative) {
        this.childRejectedReactionsNegative = childRejectedReactionsNegative;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Timestamp getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Timestamp publishAt) {
        this.publishAt = publishAt;
    }

    public boolean isUpdateImportant() {
        return updateImportant;
    }

    public void setUpdateImportant(boolean updateImportant) {
        this.updateImportant = updateImportant;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }

    public void setUpdateDescription(String updateDescription) {
        this.updateDescription = updateDescription;
    }

    public String getOperations() {
        return operations;
    }

    public void setOperations(String operations) {
        this.operations = operations;
    }

    public String getChildOperations() {
        return childOperations;
    }

    public void setChildOperations(String childOperations) {
        this.childOperations = childOperations;
    }

    public Set<EntryAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<EntryAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(EntryAttachment attachment) {
        attachments.add(attachment);
        attachment.setDraft(this);
    }

    public void removeAttachment(EntryAttachment attachment) {
        attachments.removeIf(r -> r.getId().equals(attachment.getId()));
        attachment.setDraft(null);
    }

    public boolean isAllowAnonymousChildren() {
        return allowAnonymousChildren;
    }

    public void setAllowAnonymousChildren(boolean allowAnonymousChildren) {
        this.allowAnonymousChildren = allowAnonymousChildren;
    }

}
