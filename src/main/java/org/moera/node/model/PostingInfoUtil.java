package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.AcceptedReactions;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.FeedReference;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.ReactionOperations;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.Story;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.option.Options;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.sanitizer.HtmlSanitizer;
import org.moera.node.util.SheriffUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class PostingInfoUtil {

    // for liberin models
    public static PostingInfo build(Entry posting, AccessChecker accessChecker) {
        return build(
            posting,
            posting.getCurrentRevision(),
            null,
            MediaAttachmentsProvider.RELATIONS,
            false,
            accessChecker,
            null
        );
    }

    public static PostingInfo build(
        Entry posting,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        AccessChecker accessChecker
    ) {
        return build(
            posting,
            posting.getCurrentRevision(),
            null,
            mediaAttachmentsProvider,
            false,
            accessChecker,
            null
        );
    }

    // for postings attached to media
    public static PostingInfo build(Entry posting, boolean includeSource, AccessChecker accessChecker) {
        return build(
            posting,
            posting.getCurrentRevision(),
            null,
            MediaAttachmentsProvider.NONE,
            includeSource,
            accessChecker,
            null
        );
    }

    public static PostingInfo build(
        Entry posting,
        Collection<Story> stories,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        AccessChecker accessChecker,
        Options options
    ) {
        return build(
            posting,
            posting.getCurrentRevision(),
            stories,
            mediaAttachmentsProvider,
            false,
            accessChecker,
            options
        );
    }

    public static PostingInfo build(
        Entry posting,
        Collection<Story> stories,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        boolean includeSource,
        AccessChecker accessChecker,
        Options options
    ) {
        return build(
            posting,
            posting.getCurrentRevision(),
            stories,
            mediaAttachmentsProvider,
            includeSource,
            accessChecker,
            options
        );
    }

    public static PostingInfo build(
        Entry posting,
        EntryRevision revision,
        Collection<Story> stories,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        boolean includeSource,
        AccessChecker accessChecker,
        Options options
    ) {
        PostingInfo info = new PostingInfo();
        buildTo(info, posting, revision, stories, mediaAttachmentsProvider, includeSource, accessChecker, options);
        return info;
    }

    private static void buildTo(
        PostingInfo info,
        Entry posting,
        EntryRevision revision,
        Collection<Story> stories,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        boolean includeSource,
        AccessChecker accessChecker,
        Options options
    ) {
        info.setId(posting.getId().toString());
        info.setRevisionId(revision.getId().toString());
        info.setReceiverRevisionId(revision.getReceiverRevisionId());
        info.setTotalRevisions(posting.getTotalRevisions());
        info.setReceiverName(posting.getReceiverName());
        info.setReceiverFullName(posting.getReceiverFullName());
        info.setReceiverGender(posting.getReceiverGender());
        if (posting.getReceiverAvatarMediaFile() != null) {
            info.setReceiverAvatar(AvatarImageUtil.build(
                posting.getReceiverAvatarMediaFile(), posting.getReceiverAvatarShape()
            ));
        }
        info.setReceiverPostingId(posting.getReceiverEntryId());
        info.setParentMediaId(posting.getParentMedia() != null ? posting.getParentMedia().getId().toString() : null);
        info.setOwnerName(posting.getOwnerName());
        info.setOwnerFullName(posting.getOwnerFullName());
        info.setOwnerGender(posting.getOwnerGender());
        if (posting.getOwnerAvatarMediaFile() != null) {
            info.setOwnerAvatar(
                AvatarImageUtil.build(posting.getOwnerAvatarMediaFile(), posting.getOwnerAvatarShape()));
        }
        info.setBodyPreview(new Body(revision.getBodyPreview()));
        if (includeSource && !ObjectUtils.isEmpty(revision.getBodySrc())) {
            info.setBodySrc(new Body(revision.getBodySrc()));
        }
        info.setBodySrcHash(revision.getReceiverBodySrcHash() != null
            ? revision.getReceiverBodySrcHash()
            : CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc())));
        info.setBodySrcFormat(revision.getBodySrcFormat());
        info.setBody(new Body(revision.getBody()));
        info.setBodyFormat(BodyFormat.forValue(revision.getBodyFormat()));
        info.setMedia(mediaAttachmentsProvider.getMediaAttachments(revision, posting.getReceiverName()));
        info.setHeading(revision.getHeading());
        if (!UpdateInfoUtil.isEmpty(revision)) {
            info.setUpdateInfo(UpdateInfoUtil.build(revision));
        }
        info.setCreatedAt(Util.toEpochSecond(posting.getCreatedAt()));
        info.setEditedAt(Util.toEpochSecond(posting.getEditedAt()));
        info.setDeletedAt(Util.toEpochSecond(posting.getDeletedAt()));
        info.setReceiverCreatedAt(Util.toEpochSecond(posting.getReceiverCreatedAt()));
        info.setReceiverEditedAt(Util.toEpochSecond(posting.getReceiverEditedAt()));
        info.setReceiverDeletedAt(Util.toEpochSecond(posting.getReceiverDeletedAt()));
        info.setRevisionCreatedAt(Util.toEpochSecond(revision.getCreatedAt()));
        info.setReceiverRevisionCreatedAt(Util.toEpochSecond(revision.getReceiverCreatedAt()));
        info.setDeadline(Util.toEpochSecond(posting.getDeadline()));
        info.setDigest(revision.getDigest());
        info.setSignature(revision.getSignature());
        info.setSignatureVersion(revision.getSignatureVersion());
        if (!ObjectUtils.isEmpty(stories)) {
            info.setFeedReferences(stories.stream().map(FeedReferenceUtil::build).collect(Collectors.toList()));
        }
        if (accessChecker.isPrincipal(Principal.ADMIN, Scope.OTHER)
            && posting.getBlockedInstants() != null && !posting.getBlockedInstants().isEmpty()) {
            info.setBlockedInstants(posting.getBlockedInstants().stream()
                .map(BlockedPostingInstantInfoUtil::build)
                .collect(Collectors.toList()));
        }

        PostingOperations operations = new PostingOperations();
        operations.setView(posting.getViewPrincipal(), Principal.PUBLIC);
        operations.setEdit(posting.getEditPrincipal(), Principal.OWNER);
        operations.setDelete(posting.getDeletePrincipal(), Principal.PRIVATE);
        operations.setViewComments(posting.getViewCommentsPrincipal(), Principal.PUBLIC);
        operations.setAddComment(posting.getAddCommentPrincipal(), Principal.SIGNED);
        operations.setOverrideComment(posting.getOverrideCommentPrincipal(), Principal.OWNER);
        operations.setViewReactions(posting.getViewReactionsPrincipal(), Principal.PUBLIC);
        operations.setViewNegativeReactions(posting.getViewNegativeReactionsPrincipal(), Principal.PUBLIC);
        operations.setViewReactionTotals(posting.getViewReactionTotalsPrincipal(), Principal.PUBLIC);
        operations.setViewNegativeReactionTotals(posting.getViewNegativeReactionTotalsPrincipal(), Principal.PUBLIC);
        operations.setViewReactionRatios(posting.getViewReactionRatiosPrincipal(), Principal.PUBLIC);
        operations.setViewNegativeReactionRatios(posting.getViewNegativeReactionRatiosPrincipal(), Principal.PUBLIC);
        operations.setAddReaction(posting.getAddReactionPrincipal(), Principal.SIGNED);
        operations.setAddNegativeReaction(posting.getAddNegativeReactionPrincipal(), Principal.SIGNED);
        operations.setOverrideReaction(posting.getOverrideReactionPrincipal(), Principal.OWNER);
        operations.setOverrideCommentReaction(posting.getOverrideCommentReactionPrincipal(), Principal.OWNER);
        info.setOperations(operations);

        if (!posting.isOriginal()) {
            PostingOperations receiverOperations = new PostingOperations();
            receiverOperations.setView(posting.getReceiverViewPrincipal(), Principal.PUBLIC);
            receiverOperations.setEdit(posting.getReceiverEditPrincipal(), Principal.OWNER);
            receiverOperations.setDelete(posting.getReceiverDeletePrincipal(), Principal.PRIVATE);
            receiverOperations.setViewComments(posting.getReceiverViewCommentsPrincipal(), Principal.PUBLIC);
            receiverOperations.setAddComment(posting.getReceiverAddCommentPrincipal(), Principal.SIGNED);
            receiverOperations.setOverrideComment(posting.getReceiverOverrideCommentPrincipal(), Principal.OWNER);
            receiverOperations.setViewReactions(posting.getReceiverViewReactionsPrincipal(), Principal.PUBLIC);
            receiverOperations.setViewNegativeReactions(
                posting.getReceiverViewNegativeReactionsPrincipal(), Principal.PUBLIC
            );
            receiverOperations.setViewReactionTotals(
                posting.getReceiverViewReactionTotalsPrincipal(), Principal.PUBLIC
            );
            receiverOperations.setViewNegativeReactionTotals(
                posting.getReceiverViewNegativeReactionTotalsPrincipal(), Principal.PUBLIC
            );
            receiverOperations.setViewReactionRatios(
                posting.getReceiverViewReactionRatiosPrincipal(), Principal.PUBLIC
            );
            receiverOperations.setViewNegativeReactionRatios(
                posting.getReceiverViewNegativeReactionRatiosPrincipal(), Principal.PUBLIC
            );
            receiverOperations.setAddReaction(posting.getReceiverAddReactionPrincipal(), Principal.SIGNED);
            receiverOperations.setAddNegativeReaction(
                posting.getReceiverAddNegativeReactionPrincipal(), Principal.SIGNED
            );
            receiverOperations.setOverrideReaction(posting.getReceiverOverrideReactionPrincipal(), Principal.OWNER);
            receiverOperations.setOverrideCommentReaction(
                posting.getReceiverOverrideCommentReactionPrincipal(), Principal.OWNER
            );
            info.setReceiverOperations(receiverOperations);
        }

        CommentOperations commentOperations = new CommentOperations();
        commentOperations.setView(posting.getChildOperations().getView(), Principal.UNSET);
        commentOperations.setEdit(posting.getChildOperations().getEdit(), Principal.UNSET);
        commentOperations.setDelete(posting.getChildOperations().getDelete(), Principal.UNSET);
        commentOperations.setViewReactions(posting.getChildOperations().getViewReactions(), Principal.UNSET);
        commentOperations.setViewNegativeReactions(
            posting.getChildOperations().getViewNegativeReactions(), Principal.UNSET
        );
        commentOperations.setViewReactionTotals(posting.getChildOperations().getViewReactionTotals(), Principal.UNSET);
        commentOperations.setViewNegativeReactionTotals(
            posting.getChildOperations().getViewNegativeReactionTotals(), Principal.UNSET
        );
        commentOperations.setViewReactionRatios(posting.getChildOperations().getViewReactionRatios(), Principal.UNSET);
        commentOperations.setViewNegativeReactionRatios(
            posting.getChildOperations().getViewNegativeReactionRatios(), Principal.UNSET
        );
        commentOperations.setAddReaction(posting.getChildOperations().getAddReaction(), Principal.UNSET);
        commentOperations.setAddNegativeReaction(
            posting.getChildOperations().getAddNegativeReaction(), Principal.UNSET
        );
        info.setCommentOperations(commentOperations);

        ReactionOperations reactionOperations = new ReactionOperations();
        reactionOperations.setView(posting.getReactionOperations().getView(), Principal.UNSET);
        reactionOperations.setDelete(posting.getReactionOperations().getDelete(), Principal.UNSET);
        info.setReactionOperations(reactionOperations);

        ReactionOperations commentReactionOperations = new ReactionOperations();
        commentReactionOperations.setView(posting.getChildReactionOperations().getView(), Principal.UNSET);
        commentReactionOperations.setDelete(posting.getChildReactionOperations().getDelete(), Principal.UNSET);
        info.setCommentReactionOperations(commentReactionOperations);

        fillSheriffs(info, posting, options);
        setSheriffUserListReferred(info, posting.isSheriffUserListReferred());

        AcceptedReactions acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(posting.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(posting.getAcceptedReactionsNegative());
        info.setAcceptedReactions(acceptedReactions);

        info.setReactions(ReactionTotalsInfoUtil.build(posting.getReactionTotals(), posting, accessChecker));
        info.setSources(posting.getSources() != null
            ? posting.getSources().stream().map(PostingSourceInfoUtil::build).collect(Collectors.toList())
            : Collections.emptyList());
        Principal viewComments = posting.isOriginal()
            ? posting.getViewCommentsE()
            : posting.getReceiverViewCommentsE();
        info.setTotalComments(
            accessChecker.isPrincipal(viewComments, Scope.VIEW_CONTENT) ? posting.getTotalChildren() : 0
        );
    }

    private static void fillSheriffs(PostingInfo info, Entry posting, Options options) {
        if (posting.isOriginal()) {
            fillFeedSheriffs(info, options);
            SheriffUtil.deserializeSheriffMarks(posting.getSheriffMarks()).ifPresent(marks -> {
                var sheriffMarks = info.getSheriffMarks();
                if (sheriffMarks == null) {
                    sheriffMarks = new ArrayList<>();
                }
                sheriffMarks.addAll(marks);
                info.setSheriffMarks(sheriffMarks);
            });
        } else {
            info.setSheriffs(SheriffUtil.deserializeSheriffs(posting.getReceiverSheriffs()).orElse(null));
            info.setSheriffMarks(SheriffUtil.deserializeSheriffMarks(posting.getReceiverSheriffMarks()).orElse(null));
        }
    }

    private static void fillFeedSheriffs(PostingInfo info, Options options) {
        if (info.getFeedReferences() == null || options == null) {
            return;
        }

        for (FeedReference feedReference : info.getFeedReferences()) {
            String feedName = feedReference.getFeedName();

            FeedOperations.getFeedSheriffs(options, feedName).ifPresent(feedSheriffs -> {
                var sheriffs = info.getSheriffs();
                if (sheriffs == null) {
                    sheriffs = new ArrayList<>();
                }
                sheriffs.addAll(feedSheriffs);
                info.setSheriffs(sheriffs);
            });

            FeedOperations.getFeedSheriffMarks(options, feedName).ifPresent(marks -> {
                var sheriffMarks = info.getSheriffMarks();
                if (sheriffMarks == null) {
                    sheriffMarks = new ArrayList<>();
                }
                sheriffMarks.addAll(marks);
                info.setSheriffMarks(sheriffMarks);
            });
        }
    }

    public static PostingUiInfo buildForUi(Entry posting, MediaAttachmentsProvider mediaAttachmentsProvider) {
        return buildForUi(posting, null, mediaAttachmentsProvider, null);
    }

    public static PostingUiInfo buildForUi(
        Entry posting,
        List<Story> stories,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        Options options
    ) {
        PostingUiInfo info = new PostingUiInfo();

        buildTo(
            info,
            posting,
            posting.getCurrentRevision(),
            stories,
            mediaAttachmentsProvider,
            false,
            AccessCheckers.PUBLIC,
            options
        );

        String saneBodyPreview = posting.getCurrentRevision().getSaneBodyPreview();
        if (saneBodyPreview != null) {
            setSaneBodyPreview(info, saneBodyPreview);
        } else {
            setSaneBodyPreview(
                info,
                !ObjectUtils.isEmpty(info.getBodyPreview().getText())
                    ? info.getBodyPreview().getText()
                    : info.getBody().getText()
            );
        }
        String saneBody = posting.getCurrentRevision().getSaneBody();
        setSaneBody(info, saneBody != null ? saneBody : info.getBody().getText());

        return info;
    }

    public static String getSaneBodyPreview(PostingInfo info) {
        return info.getOrCreateExtra(PostingInfoExtra::new).getSaneBodyPreview();
    }

    public static void setSaneBodyPreview(PostingInfo info, String saneBodyPreview) {
        info.getOrCreateExtra(PostingInfoExtra::new).setSaneBodyPreview(saneBodyPreview);
    }

    public static String getSaneBody(PostingInfo info) {
        return info.getOrCreateExtra(PostingInfoExtra::new).getSaneBody();
    }

    public static void setSaneBody(PostingInfo info, String saneBody) {
        info.getOrCreateExtra(PostingInfoExtra::new).setSaneBody(saneBody);
    }

    public static boolean isSheriffUserListReferred(PostingInfo info) {
        return info.getOrCreateExtra(PostingInfoExtra::new).isSheriffUserListReferred();
    }

    public static void setSheriffUserListReferred(PostingInfo info, boolean sheriffUserListReferred) {
        info.getOrCreateExtra(PostingInfoExtra::new).setSheriffUserListReferred(sheriffUserListReferred);
    }

    public static boolean isOriginal(PostingInfo info) {
        return info.getReceiverName() == null;
    }

    public static void putBlockedOperation(PostingInfo info, BlockedOperation operation) {
        if (info.getBlockedOperations() == null) {
            info.setBlockedOperations(new ArrayList<>());
        }
        switch (operation) {
            case COMMENT:
                if (!info.getBlockedOperations().contains("addComment")) {
                    info.getBlockedOperations().add("addComment");
                }
                break;
            case REACTION:
                if (!info.getBlockedOperations().contains("addReaction")) {
                    info.getBlockedOperations().add("addReaction");
                }
                if (info.getBlockedCommentOperations() == null) {
                    info.setBlockedCommentOperations(new ArrayList<>());
                }
                if (!info.getBlockedCommentOperations().contains("addReaction")) {
                    info.getBlockedCommentOperations().add("addReaction");
                }
                break;
        }
    }

    public static void putBlockedOperations(PostingInfo info, List<BlockedOperation> operations) {
        if (operations != null) {
            for (BlockedOperation operation : operations) {
                putBlockedOperation(info, operation);
            }
        }
    }

    public static void toPickedPosting(PostingInfo info, Entry posting) {
        posting.setEditedAt(Util.toTimestamp(info.getEditedAt()));
        posting.setReceiverEntryId(isOriginal(info) ? info.getId() : info.getReceiverPostingId());
        posting.setOwnerName(info.getOwnerName());
        posting.setOwnerFullName(info.getOwnerFullName());
        posting.setOwnerGender(info.getOwnerGender());
        if (info.getOwnerAvatar() != null && info.getOwnerAvatar().getShape() != null) {
            posting.setOwnerAvatarShape(info.getOwnerAvatar().getShape());
        }
        posting.setReceiverCreatedAt(
            Util.toTimestamp(isOriginal(info) ? info.getCreatedAt() : info.getReceiverCreatedAt())
        );
        posting.setReceiverEditedAt(
            Util.toTimestamp(isOriginal(info) ? info.getEditedAt() : info.getReceiverEditedAt())
        );
        posting.setReceiverDeletedAt(null);
        if (info.getAcceptedReactions() != null) {
            posting.setAcceptedReactionsPositive(info.getAcceptedReactions().getPositive());
            posting.setAcceptedReactionsNegative(info.getAcceptedReactions().getNegative());
        }
        posting.setTotalChildren(info.getTotalComments());
        // TODO visibility to a particular group of friends should be converted to something here
        // https://github.com/MoeraOrg/moera-issues/issues/207
        Principal principal = PostingOperations.getView(info.getOperations(), Principal.PUBLIC);
        posting.setViewPrincipal(principal.isFriends() || principal.isSubscribed() ? Principal.PRIVATE : principal);
        posting.setReceiverViewPrincipal(principal);
        // TODO visibility to a particular group of friends should be converted to something here
        // https://github.com/MoeraOrg/moera-issues/issues/207
        principal = PostingOperations.getEdit(info.getOperations(), Principal.OWNER);
        posting.setReceiverEditPrincipal(principal);
        principal = PostingOperations.getDelete(info.getOperations(), Principal.PRIVATE);
        posting.setReceiverDeletePrincipal(principal);
        principal = PostingOperations.getViewComments(info.getOperations(), Principal.PUBLIC);
        posting.setViewCommentsPrincipal(Principal.NONE);
        posting.setReceiverViewCommentsPrincipal(principal);
        principal = PostingOperations.getAddComment(info.getOperations(), Principal.SIGNED);
        posting.setAddCommentPrincipal(Principal.NONE);
        posting.setReceiverAddCommentPrincipal(principal);
        principal = PostingOperations.getOverrideComment(info.getOperations(), Principal.OWNER);
        posting.setReceiverOverrideCommentPrincipal(principal);
        principal = PostingOperations.getViewReactions(info.getOperations(), Principal.PUBLIC);
        posting.setViewReactionsPrincipal(Principal.NONE);
        posting.setReceiverViewReactionsPrincipal(principal);
        principal = PostingOperations.getViewNegativeReactions(info.getOperations(), Principal.PUBLIC);
        posting.setViewNegativeReactionsPrincipal(Principal.NONE);
        posting.setReceiverViewNegativeReactionsPrincipal(principal);
        principal = PostingOperations.getViewReactionTotals(info.getOperations(), Principal.PUBLIC);
        posting.setViewReactionTotalsPrincipal(principal);
        posting.setReceiverViewReactionTotalsPrincipal(principal);
        principal = PostingOperations.getViewNegativeReactionTotals(info.getOperations(), Principal.PUBLIC);
        posting.setViewNegativeReactionTotalsPrincipal(principal);
        posting.setReceiverViewNegativeReactionTotalsPrincipal(principal);
        principal = PostingOperations.getViewReactionRatios(info.getOperations(), Principal.PUBLIC);
        posting.setViewReactionRatiosPrincipal(principal);
        posting.setReceiverViewReactionRatiosPrincipal(principal);
        principal = PostingOperations.getViewNegativeReactionRatios(info.getOperations(), Principal.PUBLIC);
        posting.setViewNegativeReactionRatiosPrincipal(principal);
        posting.setReceiverViewNegativeReactionRatiosPrincipal(principal);
        principal = PostingOperations.getAddReaction(info.getOperations(), Principal.SIGNED);
        posting.setAddReactionPrincipal(Principal.NONE);
        posting.setReceiverAddReactionPrincipal(principal);
        principal = PostingOperations.getAddNegativeReaction(info.getOperations(), Principal.SIGNED);
        posting.setAddNegativeReactionPrincipal(Principal.NONE);
        posting.setReceiverAddNegativeReactionPrincipal(principal);
        principal = PostingOperations.getOverrideReaction(info.getOperations(), Principal.OWNER);
        posting.setReceiverOverrideReactionPrincipal(principal);
        principal = PostingOperations.getOverrideCommentReaction(info.getOperations(), Principal.OWNER);
        posting.setReceiverOverrideCommentReactionPrincipal(principal);
        posting.setReceiverSheriffs(SheriffUtil.serializeSheriffs(info.getSheriffs()).orElse(null));
        posting.setReceiverSheriffMarks(SheriffUtil.serializeSheriffMarks(info.getSheriffMarks()).orElse(null));
    }

    public static void toPickedEntryRevision(PostingInfo info, EntryRevision entryRevision) {
        List<MediaFileOwner> media = entryRevision.getAttachments().stream()
            .map(EntryAttachment::getMediaFileOwner)
            .collect(Collectors.toList());

        entryRevision.setReceiverRevisionId(info.getRevisionId());
        entryRevision.setBodyPreview(info.getBodyPreview().getEncoded());
        entryRevision.setSaneBodyPreview(
            HtmlSanitizer.sanitizeIfNeeded(
                !ObjectUtils.isEmpty(info.getBodyPreview().getText()) ? info.getBodyPreview() : info.getBody(),
                true,
                media
            )
        );
        entryRevision.setBodySrcFormat(info.getBodySrcFormat());
        entryRevision.setReceiverBodySrcHash(info.getBodySrcHash());
        entryRevision.setBodyFormat(info.getBodyFormat().getValue());
        entryRevision.setBody(info.getBody().getEncoded());
        entryRevision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(info.getBody(), false, media));
        entryRevision.setHeading(info.getHeading());
        entryRevision.setDescription(HeadingExtractor.extractDescription(info.getBody(), false, info.getHeading()));
        if (info.getDeletedAt() != null) {
            entryRevision.setDeletedAt(Util.now());
        }
        entryRevision.setReceiverCreatedAt(Util.toTimestamp(info.getRevisionCreatedAt()));
        entryRevision.setReceiverDeletedAt(Util.toTimestamp(info.getDeletedAt()));
        if (info.getSignature() != null && info.getSignatureVersion() != null) {
            entryRevision.setSignature(info.getSignature());
            entryRevision.setSignatureVersion(info.getSignatureVersion());
        }
    }

    public static void toOwnPosting(PostingInfo info, OwnPosting ownPosting) {
        ownPosting.setRemotePostingId(info.getId());
        ownPosting.setHeading(info.getHeading());
        ownPosting.setCreatedAt(Util.now());
    }

}
