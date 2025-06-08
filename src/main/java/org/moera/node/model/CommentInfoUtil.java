package org.moera.node.model;

import java.util.ArrayList;
import java.util.List;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.AcceptedReactions;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.ReactionOperations;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.OwnComment;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.util.SheriffUtil;
import org.moera.node.util.Util;

public class CommentInfoUtil {

    // for liberins
    public static CommentInfo build(Comment comment, AccessChecker accessChecker) {
        return build(comment, comment.getCurrentRevision(), MediaAttachmentsProvider.RELATIONS, false, accessChecker);
    }

    public static CommentInfo build(
        Comment comment, MediaAttachmentsProvider mediaAttachmentsProvider, AccessChecker accessChecker
    ) {
        return build(comment, comment.getCurrentRevision(), mediaAttachmentsProvider, false, accessChecker);
    }

    public static CommentInfo build(
        Comment comment,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        boolean includeSource,
        AccessChecker accessChecker
    ) {
        return build(comment, comment.getCurrentRevision(), mediaAttachmentsProvider, includeSource, accessChecker);
    }

    public static CommentInfo build(
        Comment comment,
        EntryRevision revision,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        boolean includeSource,
        AccessChecker accessChecker
    ) {
        CommentInfo commentInfo = new CommentInfo();
        buildTo(commentInfo, comment, revision, mediaAttachmentsProvider, includeSource, accessChecker);
        return commentInfo;
    }

    private static void buildTo(
        CommentInfo commentInfo,
        Comment comment,
        EntryRevision revision,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        boolean includeSource,
        AccessChecker accessChecker
    ) {
        commentInfo.setId(comment.getId().toString());
        commentInfo.setOwnerName(comment.getOwnerName());
        commentInfo.setOwnerFullName(comment.getOwnerFullName());
        commentInfo.setOwnerGender(comment.getOwnerGender());
        if (comment.getOwnerAvatarMediaFile() != null) {
            commentInfo.setOwnerAvatar(
                AvatarImageUtil.build(comment.getOwnerAvatarMediaFile(), comment.getOwnerAvatarShape())
            );
        }
        commentInfo.setPostingId(comment.getPosting().getId().toString());
        commentInfo.setPostingRevisionId(revision.getParent().getId().toString());
        commentInfo.setRevisionId(revision.getId().toString());
        commentInfo.setTotalRevisions(comment.getTotalRevisions());
        commentInfo.setBodyPreview(new Body(revision.getBodyPreview()));
        if (includeSource) {
            commentInfo.setBodySrc(new Body(revision.getBodySrc()));
        }
        commentInfo.setBodySrcHash(CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc())));
        commentInfo.setBodySrcFormat(revision.getBodySrcFormat());
        commentInfo.setBody(new Body(revision.getBody()));
        commentInfo.setBodyFormat(BodyFormat.forValue(revision.getBodyFormat()));
        commentInfo.setMedia(mediaAttachmentsProvider.getMediaAttachments(revision, null));
        commentInfo.setHeading(revision.getHeading());
        commentInfo.setDescription(revision.getDescription());
        if (comment.getRepliedTo() != null) {
            commentInfo.setRepliedTo(RepliedToUtil.build(comment));
        }
        commentInfo.setMoment(comment.getMoment());
        commentInfo.setCreatedAt(Util.toEpochSecond(comment.getCreatedAt()));
        commentInfo.setEditedAt(Util.toEpochSecond(comment.getEditedAt()));
        commentInfo.setDeletedAt(Util.toEpochSecond(comment.getDeletedAt()));
        commentInfo.setRevisionCreatedAt(Util.toEpochSecond(revision.getCreatedAt()));
        commentInfo.setDeadline(Util.toEpochSecond(comment.getDeadline()));
        commentInfo.setDigest(revision.getDigest());
        commentInfo.setSignature(revision.getSignature());
        commentInfo.setSignatureVersion(revision.getSignatureVersion());

        CommentOperations operations = new CommentOperations();
        operations.setView(comment.getViewCompound(), Principal.PUBLIC);
        operations.setEdit(comment.getEditCompound(), Principal.OWNER);
        operations.setDelete(comment.getDeleteCompound(), Principal.PRIVATE);
        operations.setViewReactions(comment.getViewReactionsCompound(), Principal.PUBLIC);
        operations.setViewNegativeReactions(comment.getViewNegativeReactionsCompound(), Principal.PUBLIC);
        operations.setViewReactionTotals(comment.getViewReactionTotalsCompound(), Principal.PUBLIC);
        operations.setViewNegativeReactionTotals(comment.getViewNegativeReactionTotalsCompound(), Principal.PUBLIC);
        operations.setViewReactionRatios(comment.getViewReactionRatiosCompound(), Principal.PUBLIC);
        operations.setViewNegativeReactionRatios(comment.getViewNegativeReactionRatiosCompound(), Principal.PUBLIC);
        operations.setAddReaction(comment.getAddReactionCompound(), Principal.SIGNED);
        operations.setAddNegativeReaction(comment.getAddNegativeReactionCompound(), Principal.SIGNED);
        commentInfo.setOperations(operations);

        ReactionOperations reactionOperations = new ReactionOperations();
        reactionOperations.setView(comment.getReactionOperations().getView(), Principal.UNSET);
        reactionOperations.setDelete(comment.getReactionOperations().getDelete(), Principal.UNSET);
        commentInfo.setReactionOperations(reactionOperations);

        if (accessChecker.isPrincipal(comment.getViewOperationsE(), Scope.VIEW_CONTENT)) {
            CommentOperations ownerOperations = new CommentOperations();
            ownerOperations.setView(comment.getViewPrincipal(), Principal.PUBLIC);
            ownerOperations.setEdit(comment.getEditPrincipal(), Principal.OWNER);
            ownerOperations.setDelete(comment.getDeletePrincipal(), Principal.PRIVATE);
            ownerOperations.setViewReactions(comment.getViewReactionsPrincipal(), Principal.PUBLIC);
            ownerOperations.setViewNegativeReactions(comment.getViewNegativeReactionsPrincipal(), Principal.PUBLIC);
            ownerOperations.setViewReactionTotals(comment.getViewReactionTotalsPrincipal(), Principal.PUBLIC);
            ownerOperations.setViewNegativeReactionTotals(
                comment.getViewNegativeReactionTotalsPrincipal(), Principal.PUBLIC
            );
            ownerOperations.setViewReactionRatios(comment.getViewReactionRatiosPrincipal(), Principal.PUBLIC);
            ownerOperations.setViewNegativeReactionRatios(
                comment.getViewNegativeReactionRatiosPrincipal(), Principal.PUBLIC
            );
            ownerOperations.setAddReaction(comment.getAddReactionPrincipal(), Principal.SIGNED);
            ownerOperations.setAddNegativeReaction(comment.getAddNegativeReactionPrincipal(), Principal.SIGNED);
            commentInfo.setOwnerOperations(ownerOperations);

            CommentOperations seniorOperations = new CommentOperations();
            seniorOperations.setView(comment.getParentViewPrincipal(), Principal.UNSET);
            seniorOperations.setEdit(comment.getParentEditPrincipal(), Principal.UNSET);
            seniorOperations.setDelete(comment.getParentDeletePrincipal(), Principal.UNSET);
            seniorOperations.setViewReactions(comment.getParentViewReactionsPrincipal(), Principal.UNSET);
            seniorOperations.setViewNegativeReactions(
                comment.getParentViewNegativeReactionsPrincipal(), Principal.UNSET
            );
            seniorOperations.setViewReactionTotals(comment.getParentViewReactionTotalsPrincipal(), Principal.UNSET);
            seniorOperations.setViewNegativeReactionTotals(
                comment.getParentViewNegativeReactionTotalsPrincipal(), Principal.UNSET
            );
            seniorOperations.setViewReactionRatios(comment.getParentViewReactionRatiosPrincipal(), Principal.UNSET);
            seniorOperations.setViewNegativeReactionRatios(
                comment.getParentViewNegativeReactionRatiosPrincipal(), Principal.UNSET
            );
            seniorOperations.setAddReaction(comment.getParentAddReactionPrincipal(), Principal.UNSET);
            seniorOperations.setAddNegativeReaction(comment.getParentAddNegativeReactionPrincipal(), Principal.UNSET);
            commentInfo.setSeniorOperations(seniorOperations);
        }

        commentInfo.setSheriffMarks(SheriffUtil.deserializeSheriffMarks(comment.getSheriffMarks()).orElse(null));
        setSheriffUserListReferred(commentInfo, comment.isSheriffUserListReferred());

        AcceptedReactions acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(comment.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(comment.getAcceptedReactionsNegative());
        commentInfo.setAcceptedReactions(acceptedReactions);

        commentInfo.setReactions(ReactionTotalsInfoUtil.build(comment.getReactionTotals(), comment, accessChecker));
    }

    public static CommentUiInfo buildForUi(Comment comment, MediaAttachmentsProvider mediaAttachmentsProvider) {
        CommentUiInfo info = new CommentUiInfo();

        buildTo(info, comment, comment.getCurrentRevision(), mediaAttachmentsProvider, false, AccessCheckers.PUBLIC);
        String saneBodyPreview = comment.getCurrentRevision().getSaneBodyPreview();
        setSaneBodyPreview(info, saneBodyPreview != null ? saneBodyPreview : info.getBodyPreview().getText());
        String saneBody = comment.getCurrentRevision().getSaneBody();
        setSaneBody(info, saneBody != null ? saneBody : info.getBody().getText());

        return info;
    }

    public static String getRepliedToId(CommentInfo info) {
        return info.getRepliedTo() != null ? info.getRepliedTo().getId() : null;
    }

    public static String getRepliedToRevisionId(CommentInfo info) {
        return info.getRepliedTo() != null ? info.getRepliedTo().getRevisionId() : null;
    }

    public static String getRepliedToName(CommentInfo info) {
        return info.getRepliedTo() != null ? info.getRepliedTo().getName() : null;
    }

    public static String getRepliedToFullName(CommentInfo info) {
        return info.getRepliedTo() != null ? info.getRepliedTo().getFullName() : null;
    }

    public static AvatarImage getRepliedToAvatar(CommentInfo info) {
        return info.getRepliedTo() != null ? info.getRepliedTo().getAvatar() : null;
    }

    public static void putBlockedOperations(CommentInfo info, List<BlockedOperation> operations) {
        if (operations != null) {
            if (info.getBlockedOperations() == null) {
                info.setBlockedOperations(new ArrayList<>());
            }
            for (BlockedOperation operation : operations) {
                if (operation == BlockedOperation.REACTION && !info.getBlockedOperations().contains("addReaction")) {
                    info.getBlockedOperations().add("addReaction");
                }
            }
        }
    }

    public static String getSaneBodyPreview(CommentInfo info) {
        return info.getOrCreateExtra(CommentInfoExtra::new).getSaneBodyPreview();
    }

    public static void setSaneBodyPreview(CommentInfo info, String saneBodyPreview) {
        info.getOrCreateExtra(CommentInfoExtra::new).setSaneBodyPreview(saneBodyPreview);
    }

    public static String getSaneBody(CommentInfo info) {
        return info.getOrCreateExtra(CommentInfoExtra::new).getSaneBody();
    }

    public static void setSaneBody(CommentInfo info, String saneBody) {
        info.getOrCreateExtra(CommentInfoExtra::new).setSaneBody(saneBody);
    }

    public static boolean isSheriffUserListReferred(CommentInfo info) {
        return info.getOrCreateExtra(CommentInfoExtra::new).isSheriffUserListReferred();
    }

    public static void setSheriffUserListReferred(CommentInfo info, boolean sheriffUserListReferred) {
        info.getOrCreateExtra(CommentInfoExtra::new).setSheriffUserListReferred(sheriffUserListReferred);
    }

    public static void toOwnComment(CommentInfo info, OwnComment ownComment) {
        ownComment.setRemotePostingId(info.getPostingId());
        ownComment.setRemoteCommentId(info.getId());
        ownComment.setRemoteRepliedToName(getRepliedToName(info));
        ownComment.setRemoteRepliedToFullName(getRepliedToFullName(info));
        ownComment.setHeading(info.getHeading());
        ownComment.setCreatedAt(Util.now());
    }

}
