package org.moera.node.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.auth.principal.Principal;
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
    private Map<String, Principal> operations;
    private Map<String, Principal> reactionOperations;
    private Map<String, Principal> ownerOperations;
    private Map<String, Principal> seniorOperations;
    private AcceptedReactions acceptedReactions;
    private ClientReactionInfo clientReaction;
    private ClientReactionInfo seniorReaction;
    private ReactionTotalsInfo reactions;

    public CommentInfo() {
    }

    public CommentInfo(UUID id) {
        this.id = id.toString();
    }

    public CommentInfo(Comment comment, AccessChecker accessChecker) {
        this(comment, comment.getCurrentRevision(), false, accessChecker);
    }

    public CommentInfo(Comment comment, boolean includeSource, AccessChecker accessChecker) {
        this(comment, comment.getCurrentRevision(), includeSource, accessChecker);
    }

    public CommentInfo(Comment comment, EntryRevision revision, boolean includeSource, AccessChecker accessChecker) {
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
        putOperation(operations, "view",
                comment.getViewCompound(), Principal.PUBLIC);
        putOperation(operations, "edit",
                comment.getEditCompound(), Principal.OWNER);
        putOperation(operations, "delete",
                comment.getDeleteCompound(), Principal.PRIVATE);
        putOperation(operations, "viewReactions",
                comment.getViewReactionsCompound(), Principal.PUBLIC);
        putOperation(operations, "viewNegativeReactions",
                comment.getViewNegativeReactionsCompound(), Principal.PUBLIC);
        putOperation(operations, "viewReactionTotals",
                comment.getViewReactionTotalsCompound(), Principal.PUBLIC);
        putOperation(operations, "viewNegativeReactionTotals",
                comment.getViewNegativeReactionTotalsCompound(), Principal.PUBLIC);
        putOperation(operations, "viewReactionRatios",
                comment.getViewReactionRatiosCompound(), Principal.PUBLIC);
        putOperation(operations, "viewNegativeReactionRatios",
                comment.getViewNegativeReactionRatiosCompound(), Principal.PUBLIC);
        putOperation(operations, "addReaction",
                comment.getAddReactionCompound(), Principal.SIGNED);
        putOperation(operations, "addNegativeReaction",
                comment.getAddNegativeReactionCompound(), Principal.SIGNED);

        reactionOperations = new HashMap<>();
        putOperation(reactionOperations, "view",
                comment.getReactionOperations().getView(), Principal.UNSET);
        putOperation(reactionOperations, "delete",
                comment.getReactionOperations().getDelete(), Principal.UNSET);

        if (accessChecker.isPrincipal(comment.getViewOperationsE())) {
            ownerOperations = new HashMap<>();
            putOperation(ownerOperations, "view",
                    comment.getViewPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "edit",
                    comment.getEditPrincipal(), Principal.OWNER);
            putOperation(ownerOperations, "delete",
                    comment.getDeletePrincipal(), Principal.PRIVATE);
            putOperation(ownerOperations, "viewReactions",
                    comment.getViewReactionsPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "viewNegativeReactions",
                    comment.getViewNegativeReactionsPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "viewReactionTotals",
                    comment.getViewReactionTotalsPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "viewNegativeReactionTotals",
                    comment.getViewNegativeReactionTotalsPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "viewReactionRatios",
                    comment.getViewReactionRatiosPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "viewNegativeReactionRatios",
                    comment.getViewNegativeReactionRatiosPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "addReaction",
                    comment.getAddReactionPrincipal(), Principal.SIGNED);
            putOperation(ownerOperations, "addNegativeReaction",
                    comment.getAddNegativeReactionPrincipal(), Principal.SIGNED);

            seniorOperations = new HashMap<>();
            putOperation(seniorOperations, "view",
                    comment.getParentViewPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "edit",
                    comment.getParentEditPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "delete",
                    comment.getParentDeletePrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "viewReactions",
                    comment.getParentViewReactionsPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "viewNegativeReactions",
                    comment.getParentViewNegativeReactionsPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "viewReactionTotals",
                    comment.getParentViewReactionTotalsPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "viewNegativeReactionTotals",
                    comment.getParentViewNegativeReactionTotalsPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "viewReactionRatios",
                    comment.getParentViewReactionRatiosPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "viewNegativeReactionRatios",
                    comment.getParentViewNegativeReactionRatiosPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "addReaction",
                    comment.getParentAddReactionPrincipal(), Principal.UNSET);
            putOperation(seniorOperations, "addNegativeReaction",
                    comment.getParentAddNegativeReactionPrincipal(), Principal.UNSET);
        }

        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(comment.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(comment.getAcceptedReactionsNegative());
        reactions = new ReactionTotalsInfo(comment.getReactionTotals(), comment, accessChecker);
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public static CommentInfo forUi(Comment comment) {
        CommentInfo info = new CommentInfo(comment, AccessCheckers.PUBLIC);
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

    public Map<String, Principal> getOwnerOperations() {
        return ownerOperations;
    }

    public void setOwnerOperations(Map<String, Principal> ownerOperations) {
        this.ownerOperations = ownerOperations;
    }

    public Map<String, Principal> getSeniorOperations() {
        return seniorOperations;
    }

    public void setSeniorOperations(Map<String, Principal> seniorOperations) {
        this.seniorOperations = seniorOperations;
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
