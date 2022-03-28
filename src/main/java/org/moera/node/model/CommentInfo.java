package org.moera.node.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.OwnComment;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.body.Body;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentInfo implements MediaInfo, ReactionsInfo {

    private String id;
    private String ownerName;
    private String ownerFullName;
    private AvatarImage ownerAvatar;
    private String postingId;
    private String postingRevisionId;
    private String revisionId;
    private Integer totalRevisions;
    private Body bodyPreview;

    @JsonIgnore
    private String saneBodyPreview;

    private Body bodySrc;
    private byte[] bodySrcHash;
    private SourceFormat bodySrcFormat;
    private Body body;

    @JsonIgnore
    private String saneBody;

    private String bodyFormat;
    private MediaAttachment[] media;
    private String heading;
    private RepliedTo repliedTo;
    private long moment;
    private Long createdAt;
    private Long editedAt;
    private Long deletedAt;
    private Long revisionCreatedAt;
    private Long deadline;
    private byte[] digest;
    private byte[] signature;
    private Short signatureVersion;
    private Map<String, String[]> operations;
    private AcceptedReactions acceptedReactions;
    private ClientReactionInfo clientReaction;
    private ClientReactionInfo seniorReaction;
    private ReactionTotalsInfo reactions;

    public CommentInfo() {
    }

    public CommentInfo(UUID id) {
        this.id = id.toString();
    }

    public CommentInfo(Comment comment, boolean isAdminOrOwner) {
        this(comment, comment.getCurrentRevision(), false, isAdminOrOwner);
    }

    public CommentInfo(Comment comment, boolean includeSource, boolean isAdminOrOwner) {
        this(comment, comment.getCurrentRevision(), includeSource, isAdminOrOwner);
    }

    public CommentInfo(Comment comment, EntryRevision revision, boolean includeSource, boolean isAdminOrOwner) {
        id = comment.getId().toString();
        ownerName = comment.getOwnerName();
        ownerFullName = comment.getOwnerFullName();
        if (comment.getOwnerAvatarMediaFile() != null) {
            ownerAvatar = new AvatarImage(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape());
        }
        postingId = comment.getPosting().getId().toString();
        postingRevisionId = revision.getParent().getId().toString();
        revisionId = revision.getId().toString();
        totalRevisions = comment.getTotalRevisions();
        bodyPreview = new Body(revision.getBodyPreview());
        if (includeSource) {
            bodySrc = new Body(revision.getBodySrc());
        }
        bodySrcHash = CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        media = revision.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> new MediaAttachment(ea, null))
                .toArray(MediaAttachment[]::new);
        heading = revision.getHeading();
        if (comment.getRepliedTo() != null) {
            repliedTo = new RepliedTo(comment);
        }
        moment = comment.getMoment();
        createdAt = Util.toEpochSecond(comment.getCreatedAt());
        editedAt = Util.toEpochSecond(comment.getEditedAt());
        deletedAt = Util.toEpochSecond(comment.getDeletedAt());
        revisionCreatedAt = Util.toEpochSecond(revision.getCreatedAt());
        deadline = Util.toEpochSecond(comment.getDeadline());
        digest = revision.getDigest();
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        operations = new HashMap<>();
        operations.put("edit", new String[]{"owner"});
        operations.put("delete", new String[]{"owner", "admin"});
        operations.put("revisions", new String[0]);
        operations.put("reactions",
                comment.isReactionsVisible() ? new String[]{"public"} : new String[]{"owner", "admin"});
        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(comment.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(comment.getAcceptedReactionsNegative());
        reactions = new ReactionTotalsInfo(comment.getReactionTotals(),
                isAdminOrOwner && comment.isOriginal() || comment.isReactionTotalsVisible());
    }

    public static CommentInfo forUi(Comment comment) {
        CommentInfo info = new CommentInfo(comment, false);
        String saneBodyPreview = comment.getCurrentRevision().getSaneBodyPreview();
        info.setSaneBodyPreview(saneBodyPreview != null ? saneBodyPreview : info.getBodyPreview().getText());
        String saneBody = comment.getCurrentRevision().getSaneBody();
        info.setSaneBody(saneBody != null ? saneBody : info.getBody().getText());
        return info;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getPostingRevisionId() {
        return postingRevisionId;
    }

    public void setPostingRevisionId(String postingRevisionId) {
        this.postingRevisionId = postingRevisionId;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    public Integer getTotalRevisions() {
        return totalRevisions;
    }

    public void setTotalRevisions(Integer totalRevisions) {
        this.totalRevisions = totalRevisions;
    }

    public Body getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(Body bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getSaneBodyPreview() {
        return saneBodyPreview;
    }

    public void setSaneBodyPreview(String saneBodyPreview) {
        this.saneBodyPreview = saneBodyPreview;
    }

    public Body getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(Body bodySrc) {
        this.bodySrc = bodySrc;
    }

    public byte[] getBodySrcHash() {
        return bodySrcHash;
    }

    public void setBodySrcHash(byte[] bodySrcHash) {
        this.bodySrcHash = bodySrcHash;
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

    public String getSaneBody() {
        return saneBody;
    }

    public void setSaneBody(String saneBody) {
        this.saneBody = saneBody;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    @Override
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

    public RepliedTo getRepliedTo() {
        return repliedTo;
    }

    public void setRepliedTo(RepliedTo repliedTo) {
        this.repliedTo = repliedTo;
    }

    @JsonIgnore
    public String getRepliedToId() {
        return getRepliedTo() != null ? getRepliedTo().getId() : null;
    }

    @JsonIgnore
    public String getRepliedToRevisionId() {
        return getRepliedTo() != null ? getRepliedTo().getRevisionId() : null;
    }

    @JsonIgnore
    public String getRepliedToName() {
        return getRepliedTo() != null ? getRepliedTo().getName() : null;
    }

    @JsonIgnore
    public String getRepliedToFullName() {
        return getRepliedTo() != null ? getRepliedTo().getFullName() : null;
    }

    @JsonIgnore
    public AvatarImage getRepliedToAvatar() {
        return getRepliedTo() != null ? getRepliedTo().getAvatar() : null;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
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

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getRevisionCreatedAt() {
        return revisionCreatedAt;
    }

    public void setRevisionCreatedAt(Long revisionCreatedAt) {
        this.revisionCreatedAt = revisionCreatedAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public Short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(Short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    @Override
    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
    }

    public ClientReactionInfo getClientReaction() {
        return clientReaction;
    }

    public void setClientReaction(ClientReactionInfo clientReaction) {
        this.clientReaction = clientReaction;
    }

    public ClientReactionInfo getSeniorReaction() {
        return seniorReaction;
    }

    public void setSeniorReaction(ClientReactionInfo seniorReaction) {
        this.seniorReaction = seniorReaction;
    }

    @Override
    public ReactionTotalsInfo getReactions() {
        return reactions;
    }

    public void setReactions(ReactionTotalsInfo reactions) {
        this.reactions = reactions;
    }

    public void toOwnComment(OwnComment ownComment) {
        ownComment.setRemotePostingId(postingId);
        ownComment.setRemoteCommentId(id);
        ownComment.setRemoteRepliedToName(getRepliedToName());
        ownComment.setRemoteRepliedToFullName(getRepliedToFullName());
        ownComment.setHeading(heading);
        ownComment.setCreatedAt(Util.now());
    }

}
