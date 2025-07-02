package org.moera.node.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.CommentSourceText;
import org.moera.lib.node.types.CommentText;
import org.moera.lib.node.types.MediaWithDigest;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.ChildOperations;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.text.TextConverter;
import org.moera.node.text.shorten.Shortener;
import org.moera.node.util.Util;

public class CommentTextUtil {
    
    public static CommentText build(
        String ownerName,
        String ownerFullName,
        String ownerGender,
        CommentSourceText sourceText,
        TextConverter textConverter
    ) {
        CommentText commentText = new CommentText();
        
        commentText.setOwnerName(ownerName);
        commentText.setOwnerFullName(ownerFullName);
        commentText.setOwnerGender(ownerGender);
        commentText.setOwnerAvatar(sourceText.getOwnerAvatar());
        commentText.setBodySrc(sourceText.getBodySrc());
        commentText.setBodySrcFormat(
            sourceText.getBodySrcFormat() != null
                ? sourceText.getBodySrcFormat() 
                : SourceFormat.PLAIN_TEXT
        );
        
        if (sourceText.getMedia() != null) {
            commentText.setMedia(
                sourceText.getMedia()
                    .stream()
                    .map(MediaWithDigest::getId)
                    .collect(Collectors.toList())
            );
        }
        
        commentText.setCreatedAt(Util.toEpochSecond(Util.now()));
        commentText.setRejectedReactions(sourceText.getRejectedReactions());
        commentText.setSeniorRejectedReactions(sourceText.getSeniorRejectedReactions());
        commentText.setRepliedToId(sourceText.getRepliedToId());
        
        if (commentText.getBodySrcFormat() != SourceFormat.APPLICATION) {
            Body decodedBody = textConverter.toHtml(commentText.getBodySrcFormat(), commentText.getBodySrc());
            commentText.setBody(decodedBody);
            commentText.setBodyFormat(BodyFormat.MESSAGE);
            
            Body decodedBodyPreview = Shortener.shorten(decodedBody, false);
            if (decodedBodyPreview == null) {
                decodedBodyPreview = new Body(Body.EMPTY);
            }
            commentText.setBodyPreview(decodedBodyPreview);
        } else {
            commentText.setBody(commentText.getBodySrc());
            commentText.setBodyFormat(BodyFormat.APPLICATION);
        }
        
        commentText.setOperations(sourceText.getOperations());
        commentText.setReactionOperations(sourceText.getReactionOperations());
        commentText.setSeniorOperations(sourceText.getSeniorOperations());
        
        return commentText;
    }

    public static void toEntry(CommentText commentText, Entry entry) {
        if (sameAsEntry(commentText, entry)) {
            return;
        }

        entry.setEditedAt(Util.now());
        if (commentText.getRejectedReactions() != null) {
            if (commentText.getRejectedReactions().getPositive() != null) {
                entry.setRejectedReactionsPositive(commentText.getRejectedReactions().getPositive());
            }
            if (commentText.getRejectedReactions().getNegative() != null) {
                entry.setRejectedReactionsNegative(commentText.getRejectedReactions().getNegative());
            }
        }
        if (commentText.getOwnerFullName() != null) {
            entry.setOwnerFullName(commentText.getOwnerFullName());
        }
        if (commentText.getOwnerGender() != null) {
            entry.setOwnerGender(commentText.getOwnerGender());
        }
        if (commentText.getOwnerAvatar() != null) {
            MediaFile ownerAvatarMediaFile = AvatarDescriptionUtil.getMediaFile(commentText.getOwnerAvatar());
            if (ownerAvatarMediaFile != null) {
                entry.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (commentText.getOwnerAvatar().getShape() != null) {
                entry.setOwnerAvatarShape(commentText.getOwnerAvatar().getShape());
            }
        }

        if (commentText.getOperations() != null) {
            toPrincipal(commentText.getOperations().getView(), entry::setViewPrincipal);
            toPrincipal(commentText.getOperations().getViewReactions(), entry::setViewReactionsPrincipal);
            toPrincipal(
                commentText.getOperations().getViewNegativeReactions(),
                entry::setViewNegativeReactionsPrincipal
            );
            toPrincipal(commentText.getOperations().getViewReactionTotals(), entry::setViewReactionTotalsPrincipal);
            toPrincipal(
                commentText.getOperations().getViewNegativeReactionTotals(),
                entry::setViewNegativeReactionTotalsPrincipal
            );
            toPrincipal(commentText.getOperations().getViewReactionRatios(), entry::setViewReactionRatiosPrincipal);
            toPrincipal(
                commentText.getOperations().getViewNegativeReactionRatios(),
                entry::setViewNegativeReactionRatiosPrincipal
            );
            toPrincipal(commentText.getOperations().getAddReaction(), entry::setAddReactionPrincipal);
            toPrincipal(commentText.getOperations().getAddNegativeReaction(), entry::setAddNegativeReactionPrincipal);
        }

        if (commentText.getReactionOperations() != null) {
            ChildOperations ops = entry.getReactionOperations();
            toPrincipal(commentText.getReactionOperations().getView(), ops::setView);
            toPrincipal(commentText.getReactionOperations().getDelete(), ops::setDelete);
        }

        toEntrySenior(commentText, entry);
    }

    public static void toEntrySenior(CommentText commentText, Entry entry) {
        if (commentText.getSeniorRejectedReactions() != null) {
            if (commentText.getSeniorRejectedReactions().getPositive() != null) {
                entry.setParentRejectedReactionsPositive(commentText.getSeniorRejectedReactions().getPositive());
            }
            if (commentText.getSeniorRejectedReactions().getNegative() != null) {
                entry.setParentRejectedReactionsNegative(commentText.getSeniorRejectedReactions().getNegative());
            }
        }

        if (commentText.getSeniorOperations() == null) {
            return;
        }

        toPrincipal(commentText.getSeniorOperations().getView(), entry::setParentViewPrincipal);
        toPrincipal(commentText.getSeniorOperations().getViewReactions(), entry::setParentViewReactionsPrincipal);
        toPrincipal(
            commentText.getSeniorOperations().getViewNegativeReactions(),
            entry::setParentViewNegativeReactionsPrincipal
        );
        toPrincipal(
            commentText.getSeniorOperations().getViewReactionTotals(),
            entry::setParentViewReactionTotalsPrincipal
        );
        toPrincipal(
            commentText.getSeniorOperations().getViewNegativeReactionTotals(),
            entry::setParentViewNegativeReactionTotalsPrincipal
        );
        toPrincipal(
            commentText.getSeniorOperations().getViewReactionRatios(),
            entry::setParentViewReactionRatiosPrincipal
        );
        toPrincipal(
            commentText.getSeniorOperations().getViewNegativeReactionRatios(),
            entry::setParentViewNegativeReactionRatiosPrincipal
        );
        toPrincipal(commentText.getSeniorOperations().getAddReaction(), entry::setParentAddReactionPrincipal);
        toPrincipal(
            commentText.getSeniorOperations().getAddNegativeReaction(),
            entry::setParentAddNegativeReactionPrincipal
        );
    }

    private static void toPrincipal(Principal value, Consumer<Principal> setPrincipal) {
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

    public static boolean sameAsEntry(CommentText commentText, Entry entry) {
        return
            (
                commentText.getRejectedReactions() == null
                || (
                    commentText.getRejectedReactions().getPositive() == null
                    || commentText.getRejectedReactions().getPositive().equals(entry.getRejectedReactionsPositive())
                )
                && (
                    commentText.getRejectedReactions().getNegative() == null
                    || commentText.getRejectedReactions().getNegative().equals(entry.getRejectedReactionsNegative())
                )
            )
            && (
                commentText.getSeniorRejectedReactions() == null
                || (
                    commentText.getSeniorRejectedReactions().getPositive() == null
                    || commentText.getSeniorRejectedReactions().getPositive().equals(
                        entry.getParentRejectedReactionsPositive()
                    )
                )
                && (
                    commentText.getSeniorRejectedReactions().getNegative() == null
                    || commentText.getSeniorRejectedReactions().getNegative().equals(
                        entry.getParentRejectedReactionsNegative()
                    )
                )
            )
            && (
                commentText.getOwnerFullName() == null
                || commentText.getOwnerFullName().equals(entry.getOwnerFullName())
            )
            && (commentText.getOwnerGender() == null || commentText.getOwnerGender().equals(entry.getOwnerGender()))
            && (
                AvatarDescriptionUtil.getMediaFile(commentText.getOwnerAvatar()) == null
                || entry.getOwnerAvatarMediaFile() != null
                    && AvatarDescriptionUtil.getMediaFile(commentText.getOwnerAvatar()).getId()
                        .equals(entry.getOwnerAvatarMediaFile().getId())
            )
            && (
                commentText.getOperations() == null
                || samePrincipalAs(commentText.getOperations().getView(), entry.getViewPrincipal())
                    && samePrincipalAs(
                        commentText.getOperations().getViewReactions(),
                        entry.getViewReactionsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getOperations().getViewNegativeReactions(),
                        entry.getViewNegativeReactionsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getOperations().getViewReactionTotals(),
                        entry.getViewReactionTotalsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getOperations().getViewNegativeReactionTotals(),
                        entry.getViewNegativeReactionTotalsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getOperations().getViewReactionRatios(),
                        entry.getViewReactionRatiosPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getOperations().getViewNegativeReactionRatios(),
                        entry.getViewNegativeReactionRatiosPrincipal()
                    )
                    && samePrincipalAs(commentText.getOperations().getAddReaction(), entry.getAddReactionPrincipal())
                    && samePrincipalAs(
                        commentText.getOperations().getAddNegativeReaction(),
                        entry.getAddNegativeReactionPrincipal()
                    )
            )
            && (
                commentText.getReactionOperations() == null
                || samePrincipalAs(
                    commentText.getReactionOperations().getView(),
                    entry.getReactionOperations().getView()
                )
                    && samePrincipalAs(
                        commentText.getReactionOperations().getDelete(),
                        entry.getReactionOperations().getDelete()
                    )
            )
            && (
                commentText.getSeniorOperations() == null
                || samePrincipalAs(commentText.getSeniorOperations().getView(), entry.getParentViewPrincipal())
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getViewReactions(),
                        entry.getParentViewReactionsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getViewNegativeReactions(),
                        entry.getParentViewNegativeReactionsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getViewReactionTotals(),
                        entry.getParentViewReactionTotalsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getViewNegativeReactionTotals(),
                        entry.getParentViewNegativeReactionTotalsPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getViewReactionRatios(),
                        entry.getParentViewReactionRatiosPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getViewNegativeReactionRatios(),
                        entry.getParentViewNegativeReactionRatiosPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getAddReaction(),
                        entry.getParentAddReactionPrincipal()
                    )
                    && samePrincipalAs(
                        commentText.getSeniorOperations().getAddNegativeReaction(),
                        entry.getParentAddNegativeReactionPrincipal()
                    )
            );
    }

    private static boolean samePrincipalAs(Principal value, Principal principal) {
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

    public static void toEntryRevision(
        CommentText commentText,
        EntryRevision revision,
        byte[] digest,
        TextConverter textConverter,
        List<MediaFileOwner> media
    ) {
        if (commentText.getCreatedAt() != null) {
            revision.setCreatedAt(Util.toTimestamp(commentText.getCreatedAt()));
        }
        if (commentText.getBodySrcFormat() != null) {
            revision.setBodySrcFormat(commentText.getBodySrcFormat());
        }
        if (commentText.getSignature() != null && commentText.getSignatureVersion() != null) {
            revision.setSignature(commentText.getSignature());
            revision.setSignatureVersion(commentText.getSignatureVersion());
        }
        revision.setDigest(digest);
        textConverter.toRevision(
            commentText.getBodySrc(),
            commentText.getBody(),
            commentText.getBodyFormat(),
            commentText.getBodyPreview(),
            commentText.getSignature() != null,
            media,
            true,
            revision
        );
    }

    public static boolean sameAsRevision(CommentText commentText, EntryRevision revision) {
        return
            (commentText.getBodySrcFormat() == null || commentText.getBodySrcFormat() == revision.getBodySrcFormat())
            && (
                commentText.getBodySrc() == null
                || (
                    revision.getBodySrcFormat() != SourceFormat.APPLICATION
                        ? commentText.getBodySrc().getEncoded().equals(revision.getBodySrc())
                        : commentText.getBodySrc().getEncoded().equals(revision.getBody())
                )
            )
            && (
                commentText.getMedia() == null
                || commentText.getMedia().equals(
                    revision.getAttachments().stream()
                        .map(EntryAttachment::getMediaFileOwner)
                        .map(MediaFileOwner::getId)
                        .map(UUID::toString)
                        .toList()
                )
            )
            && !(revision.getSignature() == null && commentText.getSignature() != null);
    }

}
