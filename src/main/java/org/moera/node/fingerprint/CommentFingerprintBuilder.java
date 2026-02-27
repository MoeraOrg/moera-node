package org.moera.node.fingerprint;

import java.util.UUID;
import java.util.function.Function;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.FingerprintException;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentRevisionInfo;
import org.moera.lib.node.types.CommentText;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.data.Comment;
import org.moera.node.util.Util;

public class CommentFingerprintBuilder {

    public static final short LATEST_VERSION = 1;

    public static byte[] build(Comment comment) {
        return build(LATEST_VERSION, comment);
    }

    public static byte[] build(short version, Comment comment) {
        return switch (version) {
            case 1 ->
                Fingerprints.comment1(
                    comment.getOwnerName(),
                    comment.getPosting().getCurrentRevision().getDigest(),
                    comment.getRepliedTo() != null ? comment.getRepliedTo().getCurrentRevision().getDigest() : null,
                    CryptoUtil.digest(CryptoUtil.fingerprint(comment.getCurrentRevision().getBodySrc())),
                    comment.getCurrentRevision().getBodySrcFormat().getValue(),
                    comment.getCurrentRevision().getBody(),
                    comment.getCurrentRevision().getBodyFormat(),
                    comment.getCurrentRevision().getCreatedAt(),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, comment.getCurrentRevision().getAttachments())
                    )
                );
            case 0 ->
                Fingerprints.comment0(
                    comment.getOwnerName(),
                    comment.getPosting().getCurrentRevision().getDigest(),
                    nullDigest(
                        comment.getRepliedTo() != null ? comment.getRepliedTo().getCurrentRevision().getDigest() : null
                    ),
                    CryptoUtil.digest(CryptoUtil.fingerprint(comment.getCurrentRevision().getBodySrc())),
                    comment.getCurrentRevision().getBodySrcFormat().getValue(),
                    comment.getCurrentRevision().getBody(),
                    comment.getCurrentRevision().getBodyFormat(),
                    comment.getCurrentRevision().getCreatedAt(),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, comment.getCurrentRevision().getAttachments())
                    )
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        CommentText commentText,
        Function<UUID, byte[]> mediaDigest,
        byte[] postingDigest,
        byte[] repliedToDigest
    ) {
        return build(LATEST_VERSION, commentText, mediaDigest, postingDigest, repliedToDigest);
    }

    public static byte[] build(
        short version,
        CommentText commentText,
        Function<UUID, byte[]> mediaDigest,
        byte[] postingDigest,
        byte[] repliedToDigest
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.comment1(
                    commentText.getOwnerName(),
                    postingDigest,
                    repliedToDigest,
                    CryptoUtil.digest(CryptoUtil.fingerprint(commentText.getBodySrc().getEncoded())),
                    commentText.getBodySrcFormat().getValue(),
                    commentText.getBody().getEncoded(),
                    commentText.getBodyFormat().getValue(),
                    Util.toTimestamp(commentText.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.buildFromIds(null, commentText.getMedia(), mediaDigest)
                    )
                );
            case 0 ->
                Fingerprints.comment0(
                    commentText.getOwnerName(),
                    postingDigest,
                    nullDigest(repliedToDigest),
                    CryptoUtil.digest(CryptoUtil.fingerprint(commentText.getBodySrc().getEncoded())),
                    commentText.getBodySrcFormat().getValue(),
                    commentText.getBody().getEncoded(),
                    commentText.getBodyFormat().getValue(),
                    Util.toTimestamp(commentText.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.buildFromIds(null, commentText.getMedia(), mediaDigest)
                    )
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest,
        byte[] postingFingerprint
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.comment1(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null,
                    commentInfo.getBodySrcHash(),
                    commentInfo.getBodySrcFormat().getValue(),
                    commentInfo.getBody().getEncoded(),
                    commentInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentInfo.getRevisionCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            case 0 ->
                Fingerprints.comment0(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    nullDigest(commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null),
                    commentInfo.getBodySrcHash(),
                    commentInfo.getBodySrcFormat().getValue(),
                    commentInfo.getBody().getEncoded(),
                    commentInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentInfo.getRevisionCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        CommentRevisionInfo commentRevisionInfo,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest,
        byte[] postingFingerprint
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.comment1(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null,
                    commentRevisionInfo.getBodySrcHash(),
                    commentRevisionInfo.getBodySrcFormat().getValue(),
                    commentRevisionInfo.getBody().getEncoded(),
                    commentRevisionInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentRevisionInfo.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            case 0 ->
                Fingerprints.comment0(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    nullDigest(commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null),
                    commentRevisionInfo.getBodySrcHash(),
                    commentRevisionInfo.getBodySrcFormat().getValue(),
                    commentRevisionInfo.getBody().getEncoded(),
                    commentRevisionInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentRevisionInfo.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest,
        byte[] postingFingerprint,
        byte[] repliedToDigest
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.comment1(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    repliedToDigest,
                    commentInfo.getBodySrcHash(),
                    commentInfo.getBodySrcFormat().getValue(),
                    commentInfo.getBody().getEncoded(),
                    commentInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentInfo.getRevisionCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            case 0 ->
                Fingerprints.comment0(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    nullDigest(repliedToDigest),
                    commentInfo.getBodySrcHash(),
                    commentInfo.getBodySrcFormat().getValue(),
                    commentInfo.getBody().getEncoded(),
                    commentInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentInfo.getRevisionCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        short version,
        CommentInfo commentInfo,
        CommentRevisionInfo commentRevisionInfo,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest,
        byte[] postingFingerprint,
        byte[] repliedToDigest
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.comment(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    repliedToDigest,
                    commentRevisionInfo.getBodySrcHash(),
                    commentRevisionInfo.getBodySrcFormat().getValue(),
                    commentRevisionInfo.getBody().getEncoded(),
                    commentRevisionInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentRevisionInfo.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            case 0 ->
                Fingerprints.comment(
                    commentInfo.getOwnerName(),
                    CryptoUtil.digest(postingFingerprint),
                    nullDigest(repliedToDigest),
                    commentRevisionInfo.getBodySrcHash(),
                    commentRevisionInfo.getBodySrcFormat().getValue(),
                    commentRevisionInfo.getBody().getEncoded(),
                    commentRevisionInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(commentRevisionInfo.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(null, commentInfo.getMedia(), mediaDigest)
                    )
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    private static byte[] nullDigest(byte[] digest) {
        return digest != null ? digest : CryptoUtil.digest(CryptoUtil.fingerprint(null));
    }

}
