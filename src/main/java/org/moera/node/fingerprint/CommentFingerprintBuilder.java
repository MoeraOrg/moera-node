package org.moera.node.fingerprint;

import java.util.UUID;
import java.util.function.Function;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.Fingerprints;
import org.moera.node.data.Comment;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.util.Util;

public class CommentFingerprintBuilder {

    public static final short LATEST_VERSION = 0;

    public static byte[] build(Comment comment) {
        return build(LATEST_VERSION, comment);
    }

    public static byte[] build(short version, Comment comment) {
        return Fingerprints.comment(
            comment.getOwnerName(),
            comment.getPosting().getCurrentRevision().getDigest(),
            comment.getRepliedTo() != null ? comment.getRepliedTo().getCurrentRevision().getDigest() : null,
            CryptoUtil.digest(CryptoUtil.fingerprint(comment.getCurrentRevision().getBodySrc())),
            comment.getCurrentRevision().getBodySrcFormat().getValue(),
            comment.getCurrentRevision().getBody(),
            comment.getCurrentRevision().getBodyFormat(),
            comment.getCurrentRevision().getCreatedAt(),
            (byte) 0,
            AttachmentFingerprintBuilder.build(null, comment.getCurrentRevision().getAttachments())
        );
    }

    public static byte[] build(
        CommentText commentText,
        byte[] postingDigest,
        byte[] repliedToDigest,
        Function<UUID, byte[]> mediaDigest
    ) {
        return build(LATEST_VERSION, commentText, postingDigest, repliedToDigest, mediaDigest);
    }

    public static byte[] build(
        short version,
        CommentText commentText,
        byte[] postingDigest,
        byte[] repliedToDigest,
        Function<UUID, byte[]> mediaDigest
    ) {
        return Fingerprints.comment(
            commentText.getOwnerName(),
            postingDigest,
            repliedToDigest,
            CryptoUtil.digest(CryptoUtil.fingerprint(commentText.getBodySrc())),
            commentText.getBodySrcFormat().getValue(),
            commentText.getBody(),
            commentText.getBodyFormat(),
            Util.toTimestamp(commentText.getCreatedAt()),
            (byte) 0,
            AttachmentFingerprintBuilder.build(null, commentText.getMedia(), mediaDigest)
        );
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        byte[] postingFingerprint,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        return Fingerprints.comment(
            commentInfo.getOwnerName(),
            CryptoUtil.digest(postingFingerprint),
            commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null,
            commentInfo.getBodySrcHash(),
            commentInfo.getBodySrcFormat().getValue(),
            commentInfo.getBody().getEncoded(),
            commentInfo.getBodyFormat(),
            Util.toTimestamp(commentInfo.getRevisionCreatedAt()),
            (byte) 0,
            AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
        );
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        CommentRevisionInfo commentRevisionInfo,
        Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        byte[] postingParentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> postingMediaDigest
    ) {
        return Fingerprints.comment(
            commentInfo.getOwnerName(),
            CryptoUtil.digest(PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                postingParentMediaDigest,
                postingMediaDigest
            )),
            commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null,
            commentRevisionInfo.getBodySrcHash(),
            commentRevisionInfo.getBodySrcFormat().getValue(),
            commentRevisionInfo.getBody().getEncoded(),
            commentRevisionInfo.getBodyFormat(),
            Util.toTimestamp(commentRevisionInfo.getCreatedAt()),
            (byte) 0,
            AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), commentMediaDigest)
        );
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        byte[] postingParentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> postingMediaDigest,
        byte[] repliedToDigest
    ) {
        return Fingerprints.comment(
            commentInfo.getOwnerName(),
            CryptoUtil.digest(PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                postingParentMediaDigest,
                postingMediaDigest
            )),
            repliedToDigest,
            commentInfo.getBodySrcHash(),
            commentInfo.getBodySrcFormat().getValue(),
            commentInfo.getBody().getEncoded(),
            commentInfo.getBodyFormat(),
            Util.toTimestamp(commentInfo.getRevisionCreatedAt()),
            (byte) 0,
            AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), commentMediaDigest)
        );
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        CommentRevisionInfo commentRevisionInfo,
        Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        byte[] postingParentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> postingMediaDigest,
        byte[] repliedToDigest
    ) {
        return Fingerprints.comment(
            commentInfo.getOwnerName(),
            CryptoUtil.digest(PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                postingParentMediaDigest,
                postingMediaDigest
            )),
            repliedToDigest,
            commentRevisionInfo.getBodySrcHash(),
            commentRevisionInfo.getBodySrcFormat().getValue(),
            commentRevisionInfo.getBody().getEncoded(),
            commentRevisionInfo.getBodyFormat(),
            Util.toTimestamp(commentRevisionInfo.getCreatedAt()),
            (byte) 0,
            AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), commentMediaDigest)
        );
    }

}
