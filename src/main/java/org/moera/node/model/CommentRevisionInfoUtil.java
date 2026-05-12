package org.moera.node.model;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.CommentRevisionInfo;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class CommentRevisionInfoUtil {

    public static CommentRevisionInfo build(
        Comment comment,
        EntryRevision revision,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        Options options,
        AccessChecker accessChecker
    ) {
        CommentRevisionInfo info = new CommentRevisionInfo();

        info.setId(revision.getId().toString());
        info.setPostingRevisionId(revision.getParent().getId().toString());
        info.setBodyPreview(new Body(revision.getBodyPreview()));
        info.setBodySrcHash(revision.getReceiverBodySrcHash() != null
                ? revision.getReceiverBodySrcHash()
                : CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc())));
        info.setBodySrcFormat(revision.getBodySrcFormat());
        info.setBody(new Body(revision.getBody()));
        info.setBodyFormat(BodyFormat.forValue(revision.getBodyFormat()));
        var grantSupplier = options != null ? new MediaGrantGenerator(options) : null;
        info.setMedia(mediaAttachmentsProvider.getMediaAttachments(revision, grantSupplier));
        info.setHeading(revision.getHeading());
        info.setDescription(revision.getDescription());
        info.setCreatedAt(Util.toEpochSecond(revision.getCreatedAt()));
        info.setDeletedAt(Util.toEpochSecond(revision.getDeletedAt()));
        info.setDeadline(Util.toEpochSecond(revision.getDeadline()));
        info.setDigest(revision.getDigest());
        info.setSignature(revision.getSignature());
        info.setSignatureVersion(revision.getSignatureVersion());
        info.setReactions(ReactionTotalsInfoUtil.build(revision.getReactionTotals(), comment, accessChecker));

        return info;
    }

}
