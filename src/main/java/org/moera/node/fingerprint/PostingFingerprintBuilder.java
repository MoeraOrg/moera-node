package org.moera.node.fingerprint;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.FingerprintException;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.SourceFormat;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.util.Util;

public class PostingFingerprintBuilder {

    public static final short LATEST_VERSION = 1;

    public static byte[] build(Posting posting, EntryRevision revision) {
        return build(LATEST_VERSION, posting, revision);
    }

    public static byte[] build(short version, Posting posting, EntryRevision revision) {
        return switch (version) {
            case 1 ->
                Fingerprints.posting1(
                    // TODO it should be posting.receiverName, if it is not null, and node name otherwise,
                    // to support postings in group nodes (?)
                    posting.getOwnerName(),
                    posting.getOwnerName(),
                    posting.isOriginal()
                        ? CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc()))
                        : revision.getReceiverBodySrcHash(),
                    revision.getBodySrcFormat().getValue(),
                    revision.getBody(),
                    revision.getBodyFormat(),
                    posting.isOriginal() ? revision.getCreatedAt() : revision.getReceiverCreatedAt(),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(posting.getParentMedia(), revision.getAttachments())
                    )
                );
            case 0 ->
                Fingerprints.posting0(
                    // TODO it should be posting.receiverName, if it is not null, and node name otherwise,
                    // to support postings in group nodes
                    posting.getOwnerName(),
                    posting.getOwnerName(),
                    posting.isOriginal()
                        ? CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc()))
                        : revision.getReceiverBodySrcHash(),
                    revision.getBodySrcFormat().getValue(),
                    revision.getBody(),
                    revision.getBodyFormat(),
                    posting.isOriginal() ? revision.getCreatedAt() : revision.getReceiverCreatedAt(),
                    (byte) 0,
                    (byte) 0
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        short version,
        PostingInfo postingInfo,
        byte[] parentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.posting1(
                    postingInfo.getOwnerName(),
                    postingInfo.getOwnerName(),
                    postingInfo.getBodySrcHash(),
                    SourceFormat.toValue(postingInfo.getBodySrcFormat()),
                    postingInfo.getBody().getEncoded(),
                    postingInfo.getBodyFormat(),
                    Util.toTimestamp(
                        postingInfo.isOriginal()
                            ? postingInfo.getRevisionCreatedAt()
                            : postingInfo.getReceiverRevisionCreatedAt()
                    ),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(
                            parentMediaDigest, Arrays.asList(postingInfo.getMedia()), mediaDigest
                        )
                    )
                );
            case 0 ->
                Fingerprints.posting0(
                    postingInfo.getOwnerName(),
                    postingInfo.getOwnerName(),
                    postingInfo.getBodySrcHash(),
                    SourceFormat.toValue(postingInfo.getBodySrcFormat()),
                    postingInfo.getBody().getEncoded(),
                    postingInfo.getBodyFormat(),
                    Util.toTimestamp(
                        postingInfo.isOriginal()
                            ? postingInfo.getRevisionCreatedAt()
                            : postingInfo.getReceiverRevisionCreatedAt()
                    ),
                    (byte) 0,
                    (byte) 0
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(
        short version,
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        byte[] parentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.posting1(
                    postingInfo.getOwnerName(),
                    postingInfo.getOwnerName(),
                    postingRevisionInfo.getBodySrcHash(),
                    SourceFormat.toValue(postingRevisionInfo.getBodySrcFormat()),
                    postingRevisionInfo.getBody().getEncoded(),
                    postingRevisionInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(
                        postingInfo.isOriginal()
                            ? postingRevisionInfo.getCreatedAt()
                            : postingRevisionInfo.getReceiverCreatedAt()
                    ),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(
                            parentMediaDigest, postingRevisionInfo.getMedia(), mediaDigest
                        )
                    )
                );
            case 0 ->
                Fingerprints.posting0(
                    postingInfo.getOwnerName(),
                    postingInfo.getOwnerName(),
                    postingRevisionInfo.getBodySrcHash(),
                    SourceFormat.toValue(postingRevisionInfo.getBodySrcFormat()),
                    postingRevisionInfo.getBody().getEncoded(),
                    postingRevisionInfo.getBodyFormat().getValue(),
                    Util.toTimestamp(
                        postingInfo.isOriginal()
                            ? postingRevisionInfo.getCreatedAt()
                            : postingRevisionInfo.getReceiverCreatedAt()
                    ),
                    (byte) 0,
                    (byte) 0
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

    public static byte[] build(PostingText postingText, byte[] parentMediaDigest, Function<UUID, byte[]> mediaDigest) {
        return build(LATEST_VERSION, postingText, parentMediaDigest, mediaDigest);
    }

    public static byte[] build(
        short version,
        PostingText postingText,
        byte[] parentMediaDigest,
        Function<UUID, byte[]> mediaDigest
    ) {
        return switch (version) {
            case 1 ->
                Fingerprints.posting1(
                    postingText.getOwnerName(),
                    postingText.getOwnerName(),
                    CryptoUtil.digest(CryptoUtil.fingerprint(postingText.getBodySrc())),
                    postingText.getBodySrcFormat().getValue(),
                    postingText.getBody(),
                    postingText.getBodyFormat(),
                    Util.toTimestamp(postingText.getCreatedAt()),
                    (byte) 0,
                    CryptoUtil.digest(
                        AttachmentFingerprintBuilder.build(parentMediaDigest, postingText.getMedia(), mediaDigest)
                    )
                );
            case 0 ->
                Fingerprints.posting0(
                    postingText.getOwnerName(),
                    postingText.getOwnerName(),
                    CryptoUtil.digest(CryptoUtil.fingerprint(postingText.getBodySrc())),
                    postingText.getBodySrcFormat().getValue(),
                    postingText.getBody(),
                    postingText.getBodyFormat(),
                    Util.toTimestamp(postingText.getCreatedAt()),
                    (byte) 0,
                    (byte) 0
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

}
