package org.moera.node.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.MediaWithDigest;
import org.moera.lib.node.types.PostingSourceText;
import org.moera.lib.node.types.PostingText;
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
import org.springframework.util.ObjectUtils;

public class PostingTextUtil {

    public static PostingText build(
        String ownerName,
        String ownerFullName,
        String ownerGender,
        PostingSourceText sourceText,
        TextConverter textConverter
    ) {
        PostingText postingText = new PostingText();
        
        postingText.setOwnerName(ownerName);
        postingText.setOwnerFullName(ownerFullName);
        postingText.setOwnerGender(ownerGender);
        postingText.setOwnerAvatar(sourceText.getOwnerAvatar());
        postingText.setBodySrc(sourceText.getBodySrc());
        postingText.setBodySrcFormat(sourceText.getBodySrcFormat());
        if (postingText.getBodySrc() != null && postingText.getBodySrcFormat() == null) {
            postingText.setBodySrcFormat(SourceFormat.PLAIN_TEXT);
        }
        
        postingText.setMedia(
            sourceText.getMedia() != null
                ? sourceText.getMedia().stream()
                    .map(MediaWithDigest::getId)
                    .collect(Collectors.toList())
                : null
        );
            
        postingText.setCreatedAt(Util.toEpochSecond(Util.now()));
        postingText.setRejectedReactions(sourceText.getRejectedReactions());
        postingText.setCommentRejectedReactions(sourceText.getCommentRejectedReactions());

        if (postingText.getBodySrc() != null) {
            if (postingText.getBodySrcFormat() != SourceFormat.APPLICATION) {
                Body decodedBody = textConverter.toHtml(postingText.getBodySrcFormat(), postingText.getBodySrc());
                postingText.setBody(decodedBody);
                postingText.setBodyFormat(BodyFormat.MESSAGE);
                Body decodedBodyPreview = Shortener.shorten(decodedBody, false);
                if (decodedBodyPreview == null) {
                    decodedBodyPreview = new Body(Body.EMPTY);
                }
                postingText.setBodyPreview(decodedBodyPreview);
            } else {
                postingText.setBody(postingText.getBodySrc());
                postingText.setBodyFormat(BodyFormat.APPLICATION);
            }
        }

        postingText.setAllowAnonymousComments(sourceText.getAllowAnonymousComments());
        postingText.setOperations(sourceText.getOperations());
        postingText.setCommentOperations(sourceText.getCommentOperations());
        postingText.setReactionOperations(sourceText.getReactionOperations());
        postingText.setCommentReactionOperations(sourceText.getCommentReactionOperations());
        
        return postingText;
    }

    public static void toEntry(PostingText postingText, Entry entry) {
        if (sameAsEntry(postingText, entry)) {
            return;
        }

        entry.setEditedAt(Util.now());
        if (postingText.getRejectedReactions() != null) {
            if (postingText.getRejectedReactions().getPositive() != null) {
                entry.setRejectedReactionsPositive(postingText.getRejectedReactions().getPositive());
            }
            if (postingText.getRejectedReactions().getNegative() != null) {
                entry.setRejectedReactionsNegative(postingText.getRejectedReactions().getNegative());
            }
        }
        if (postingText.getCommentRejectedReactions() != null) {
            if (postingText.getCommentRejectedReactions().getPositive() != null) {
                entry.setChildRejectedReactionsPositive(postingText.getCommentRejectedReactions().getPositive());
            }
            if (postingText.getCommentRejectedReactions().getNegative() != null) {
                entry.setChildRejectedReactionsNegative(postingText.getCommentRejectedReactions().getNegative());
            }
        }
        if (postingText.getOwnerName() != null) {
            entry.setOwnerName(postingText.getOwnerName());
        }
        if (postingText.getOwnerFullName() != null) {
            entry.setOwnerFullName(postingText.getOwnerFullName());
        }
        if (postingText.getOwnerGender() != null) {
            entry.setOwnerGender(postingText.getOwnerGender());
        }
        if (postingText.getOwnerAvatar() != null) {
            MediaFile ownerAvatarMediaFile = AvatarDescriptionUtil.getMediaFile(postingText.getOwnerAvatar());
            if (ownerAvatarMediaFile != null) {
                entry.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (postingText.getOwnerAvatar().getShape() != null) {
                entry.setOwnerAvatarShape(postingText.getOwnerAvatar().getShape());
            }
        }
        if (postingText.getAllowAnonymousComments() != null) {
            entry.setAllowAnonymousChildren(postingText.getAllowAnonymousComments());
        }
        if (postingText.getExternalSourceUri() != null) {
            entry.setExternalSourceUri(postingText.getExternalSourceUri());
        }

        if (entry.getParentMedia() == null) {
            if (postingText.getOperations() != null) {
                toPrincipal(postingText.getOperations().getView(), entry::setViewPrincipal);
                toPrincipal(postingText.getOperations().getViewComments(), entry::setViewCommentsPrincipal);
                toPrincipal(postingText.getOperations().getAddComment(), entry::setAddCommentPrincipal);
                toPrincipal(postingText.getOperations().getViewReactions(), entry::setViewReactionsPrincipal);
                toPrincipal(
                    postingText.getOperations().getViewNegativeReactions(),
                    entry::setViewNegativeReactionsPrincipal
                );
                toPrincipal(postingText.getOperations().getViewReactionTotals(), entry::setViewReactionTotalsPrincipal);
                toPrincipal(
                    postingText.getOperations().getViewNegativeReactionTotals(),
                    entry::setViewNegativeReactionTotalsPrincipal
                );
                toPrincipal(postingText.getOperations().getViewReactionRatios(), entry::setViewReactionRatiosPrincipal);
                toPrincipal(
                    postingText.getOperations().getViewNegativeReactionRatios(),
                    entry::setViewNegativeReactionRatiosPrincipal
                );
                toPrincipal(postingText.getOperations().getAddReaction(), entry::setAddReactionPrincipal);
                toPrincipal(
                    postingText.getOperations().getAddNegativeReaction(),
                    entry::setAddNegativeReactionPrincipal
                );
            }

            if (postingText.getCommentOperations() != null) {
                ChildOperations ops = entry.getChildOperations();
                toPrincipal(postingText.getCommentOperations().getView(), ops::setView);
                toPrincipal(postingText.getCommentOperations().getEdit(), ops::setEdit);
                toPrincipal(postingText.getCommentOperations().getDelete(), ops::setDelete);
                toPrincipal(postingText.getCommentOperations().getViewReactions(), ops::setViewReactions);
                toPrincipal(
                    postingText.getCommentOperations().getViewNegativeReactions(),
                    ops::setViewNegativeReactions
                );
                toPrincipal(postingText.getCommentOperations().getViewReactionTotals(), ops::setViewReactionTotals);
                toPrincipal(
                    postingText.getCommentOperations().getViewNegativeReactionTotals(),
                    ops::setViewNegativeReactionTotals
                );
                toPrincipal(postingText.getCommentOperations().getViewReactionRatios(), ops::setViewReactionRatios);
                toPrincipal(
                    postingText.getCommentOperations().getViewNegativeReactionRatios(),
                    ops::setViewNegativeReactionRatios
                );
                toPrincipal(postingText.getCommentOperations().getAddReaction(), ops::setAddReaction);
                toPrincipal(postingText.getCommentOperations().getAddNegativeReaction(), ops::setAddNegativeReaction);
                toPrincipal(postingText.getCommentOperations().getOverrideReaction(), ops::setOverrideReaction);
            }

            if (postingText.getReactionOperations() != null) {
                ChildOperations ops = entry.getReactionOperations();
                toPrincipal(postingText.getReactionOperations().getView(), ops::setView);
                toPrincipal(postingText.getReactionOperations().getDelete(), ops::setDelete);
            }

            if (postingText.getCommentReactionOperations() != null) {
                ChildOperations ops = entry.getChildReactionOperations();
                toPrincipal(postingText.getCommentReactionOperations().getView(), ops::setView);
                toPrincipal(postingText.getCommentReactionOperations().getDelete(), ops::setDelete);
            }
        }
    }

    private static void toPrincipal(Principal value, Consumer<Principal> setPrincipal) {
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

    public static boolean sameAsEntry(PostingText postingText, Entry entry) {
        return
            (
                postingText.getRejectedReactions() == null
                || (
                    postingText.getRejectedReactions().getPositive() == null
                    || postingText.getRejectedReactions().getPositive().equals(entry.getRejectedReactionsPositive())
                )
                && (
                    postingText.getRejectedReactions().getNegative() == null
                    || postingText.getRejectedReactions().getNegative().equals(entry.getRejectedReactionsNegative())
                )
            )
            && (
                postingText.getCommentRejectedReactions() == null
                || (
                    postingText.getCommentRejectedReactions().getPositive() == null
                    || postingText.getCommentRejectedReactions().getPositive().equals(
                        entry.getChildRejectedReactionsPositive()
                    )
                )
                && (
                    postingText.getCommentRejectedReactions().getNegative() == null
                    || postingText.getCommentRejectedReactions().getNegative().equals(
                        entry.getChildRejectedReactionsNegative()
                    )
                )
            )
            && (postingText.getOwnerName() == null || postingText.getOwnerName().equals(entry.getOwnerName()))
            && (
                postingText.getOwnerFullName() == null
                || postingText.getOwnerFullName().equals(entry.getOwnerFullName())
            )
            && (postingText.getOwnerGender() == null || postingText.getOwnerGender().equals(entry.getOwnerGender()))
            && (
                AvatarDescriptionUtil.getMediaFile(postingText.getOwnerAvatar()) == null
                || entry.getOwnerAvatarMediaFile() != null
                && AvatarDescriptionUtil.getMediaFile(postingText.getOwnerAvatar()).getId()
                        .equals(entry.getOwnerAvatarMediaFile().getId())
            )
            && (
                postingText.getAllowAnonymousComments() == null
                || postingText.getAllowAnonymousComments() == entry.isAllowAnonymousChildren()
            )
            && (
                postingText.getExternalSourceUri() == null
                || postingText.getExternalSourceUri().equals(entry.getExternalSourceUri())
            )
            && (
                postingText.getOperations() == null
                || samePrincipalAs(postingText.getOperations().getView(), entry.getViewPrincipal())
                && samePrincipalAs(postingText.getOperations().getViewComments(), entry.getViewCommentsPrincipal())
                && samePrincipalAs(postingText.getOperations().getAddComment(), entry.getAddCommentPrincipal())
                && samePrincipalAs(postingText.getOperations().getViewReactions(), entry.getViewReactionsPrincipal())
                && samePrincipalAs(
                    postingText.getOperations().getViewNegativeReactions(),
                    entry.getViewNegativeReactionsPrincipal()
                )
                && samePrincipalAs(
                    postingText.getOperations().getViewReactionTotals(),
                    entry.getViewReactionTotalsPrincipal()
                )
                && samePrincipalAs(
                    postingText.getOperations().getViewNegativeReactionTotals(),
                    entry.getViewNegativeReactionTotalsPrincipal()
                )
                && samePrincipalAs(
                    postingText.getOperations().getViewReactionRatios(),
                    entry.getViewReactionRatiosPrincipal()
                )
                && samePrincipalAs(
                    postingText.getOperations().getViewNegativeReactionRatios(),
                    entry.getViewNegativeReactionRatiosPrincipal()
                )
                && samePrincipalAs(postingText.getOperations().getAddReaction(), entry.getAddReactionPrincipal())
                && samePrincipalAs(
                    postingText.getOperations().getAddNegativeReaction(),
                    entry.getAddNegativeReactionPrincipal()
                )
            )
            && (postingText.getCommentOperations() == null
                || samePrincipalAs(postingText.getCommentOperations().getView(), entry.getChildOperations().getView())
                && samePrincipalAs(postingText.getCommentOperations().getEdit(), entry.getChildOperations().getEdit())
                && samePrincipalAs(
                    postingText.getCommentOperations().getDelete(),
                    entry.getChildOperations().getDelete()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getViewReactions(),
                    entry.getChildOperations().getViewReactions()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getViewNegativeReactions(),
                    entry.getChildOperations().getViewNegativeReactions()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getViewReactionTotals(),
                    entry.getChildOperations().getViewReactionTotals()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getViewNegativeReactionTotals(),
                    entry.getChildOperations().getViewNegativeReactionTotals()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getViewReactionRatios(),
                    entry.getChildOperations().getViewReactionRatios()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getViewNegativeReactionRatios(),
                    entry.getChildOperations().getViewNegativeReactionRatios()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getAddReaction(),
                    entry.getChildOperations().getAddReaction()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getAddNegativeReaction(),
                    entry.getChildOperations().getAddNegativeReaction()
                )
                && samePrincipalAs(
                    postingText.getCommentOperations().getOverrideReaction(),
                    entry.getChildOperations().getOverrideReaction()
                )
            )
            && (postingText.getReactionOperations() == null
                || samePrincipalAs(
                    postingText.getReactionOperations().getView(),
                    entry.getReactionOperations().getView()
                )
                && samePrincipalAs(
                    postingText.getReactionOperations().getDelete(),
                    entry.getReactionOperations().getDelete()
                )
            )
            && (postingText.getCommentReactionOperations() == null
                || samePrincipalAs(
                    postingText.getCommentReactionOperations().getView(),
                    entry.getChildReactionOperations().getView()
                )
                && samePrincipalAs(
                    postingText.getCommentReactionOperations().getDelete(),
                    entry.getChildReactionOperations().getDelete()
                )
            );
    }

    public static boolean sameViewComments(PostingText postingText, Entry entry) {
        return postingText.getOperations() == null
            || samePrincipalAs(postingText.getOperations().getView(), entry.getViewPrincipal())
            && samePrincipalAs(postingText.getOperations().getViewComments(), entry.getViewCommentsPrincipal());
    }

    private static boolean samePrincipalAs(Principal value, Principal principal) {
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

    public static void toEntryRevision(
        PostingText postingText,
        EntryRevision revision,
        byte[] digest,
        TextConverter textConverter,
        List<MediaFileOwner> media
    ) {
        if (postingText.getCreatedAt() != null) {
            revision.setCreatedAt(Util.toTimestamp(postingText.getCreatedAt()));
        }
        if (postingText.getBodySrcFormat() != null) {
            revision.setBodySrcFormat(postingText.getBodySrcFormat());
        }
        if (postingText.getSignature() != null && postingText.getSignatureVersion() != null) {
            revision.setSignature(postingText.getSignature());
            revision.setSignatureVersion(postingText.getSignatureVersion());
        }
        revision.setDigest(digest);
        textConverter.toRevision(
            postingText.getBodySrc(),
            postingText.getBody(),
            postingText.getBodyFormat(),
            postingText.getBodyPreview(),
            postingText.getSignature() != null,
            media,
            false,
            false,
            revision
        );

        if (postingText.getUpdateInfo() != null) {
            if (postingText.getUpdateInfo().getImportant() != null) {
                revision.setUpdateImportant(postingText.getUpdateInfo().getImportant());
            }
            if (postingText.getUpdateInfo().getDescription() != null) {
                revision.setUpdateDescription(postingText.getUpdateInfo().getDescription());
            }
        }
    }

    public static boolean sameAsRevision(PostingText postingText, EntryRevision revision) {
        return
            (
                ObjectUtils.isEmpty(postingText.getBodySrcFormat())
                || postingText.getBodySrcFormat() == revision.getBodySrcFormat()
            )
            && (
                postingText.getBodySrc() == null
                || (
                    revision.getBodySrcFormat() != SourceFormat.APPLICATION
                        ? postingText.getBodySrc().getEncoded().equals(revision.getBodySrc())
                        : postingText.getBodySrc().getEncoded().equals(revision.getBody()))
                )
            && (
                postingText.getMedia() == null
                || postingText.getMedia().equals(
                    revision.getAttachments().stream()
                        .map(EntryAttachment::getMediaFileOwner)
                        .map(MediaFileOwner::getId)
                        .map(UUID::toString)
                        .toList()
                )
            )
            && !(revision.getSignature() == null && postingText.getSignature() != null);
    }

}
