package org.moera.node.fingerprint;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.ReactionAttributes;
import org.moera.lib.node.types.ReactionDescription;
import org.moera.lib.node.types.ReactionInfo;

public class ReactionFingerprintBuilder {

    public static final short LATEST_VERSION = 0;

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
        byte[] entryFingerprint
    ) {
        return Fingerprints.reaction(
            reactionInfo.getOwnerName(),
            CryptoUtil.digest(entryFingerprint),
            Boolean.TRUE.equals(reactionInfo.getNegative()),
            reactionInfo.getEmoji()
        );
    }

}
