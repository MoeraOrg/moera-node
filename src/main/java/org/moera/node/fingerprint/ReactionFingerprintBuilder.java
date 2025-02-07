package org.moera.node.fingerprint;

import java.util.function.Function;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.Fingerprints;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;

public class ReactionFingerprintBuilder {

    public static final short LATEST_VERSION = 0;

    public static byte[] build(ReactionDescription description, byte[] entryDigest) {
        return build(LATEST_VERSION, description, entryDigest);
    }

    public static byte[] build(short version, ReactionDescription description, byte[] entryDigest) {
        return Fingerprints.reaction(
            description.getOwnerName(),
            entryDigest,
            description.isNegative(),
            description.getEmoji()
        );
    }

    public static byte[] build(String ownerName, ReactionAttributes attributes, byte[] entryFingerprint) {
        return build(LATEST_VERSION, ownerName, attributes, entryFingerprint);
    }

    public static byte[] build(
        short version,
        String ownerName,
        ReactionAttributes attributes,
        byte[] entryFingerprint
    ) {
        return Fingerprints.reaction(
            ownerName,
            CryptoUtil.digest(entryFingerprint),
            attributes.isNegative(),
            attributes.getEmoji()
        );
    }

    public static byte[] build(
        short version,
        ReactionInfo reactionInfo,
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        byte[] postingParentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> postingMediaDigest
    ) {
        return Fingerprints.reaction(
            reactionInfo.getOwnerName(),
            CryptoUtil.digest(PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                postingParentMediaDigest,
                postingMediaDigest
            )),
            reactionInfo.isNegative(),
            reactionInfo.getEmoji()
        );
    }

    public static byte[] build(
        short version,
        ReactionInfo reactionInfo,
        CommentInfo commentInfo,
        CommentRevisionInfo commentRevisionInfo,
        Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        byte[] postingParentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> postingMediaDigest
    ) {
        return Fingerprints.reaction(
            reactionInfo.getOwnerName(),
            CryptoUtil.digest(CommentFingerprintBuilder.build(
                commentRevisionInfo.getSignatureVersion(),
                commentInfo,
                commentRevisionInfo,
                commentMediaDigest,
                postingInfo,
                postingRevisionInfo,
                postingParentMediaDigest,
                postingMediaDigest
            )),
            reactionInfo.isNegative(),
            reactionInfo.getEmoji()
        );
    }

}
