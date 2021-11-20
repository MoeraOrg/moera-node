package org.moera.node.fingerprint;

import java.util.function.Function;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;

@FingerprintVersion(objectType = FingerprintObjectType.REACTION, version = 0)
public class ReactionFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.REACTION.name();
    public String ownerName;
    public Digest<Fingerprint> entryFingerprint = new Digest<>();
    public boolean negative;
    public int emoji;

    public ReactionFingerprint(ReactionDescription description, byte[] entryDigest) {
        super(0);
        ownerName = description.getOwnerName();
        entryFingerprint.setDigest(entryDigest);
        negative = description.isNegative();
        emoji = description.getEmoji();
    }

    public ReactionFingerprint(String ownerName, ReactionAttributes attributes, Fingerprint entryFingerprint) {
        super(0);
        this.ownerName = ownerName;
        this.entryFingerprint.setValue(entryFingerprint);
        negative = attributes.isNegative();
        emoji = attributes.getEmoji();
    }

    public ReactionFingerprint(ReactionInfo reactionInfo, PostingInfo postingInfo,
                               PostingRevisionInfo postingRevisionInfo,
                               Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        super(0);
        ownerName = reactionInfo.getOwnerName();
        entryFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingMediaDigest));
        negative = reactionInfo.isNegative();
        emoji = reactionInfo.getEmoji();
    }

    public ReactionFingerprint(ReactionInfo reactionInfo, CommentInfo commentInfo,
                               CommentRevisionInfo commentRevisionInfo,
                               Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                               PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                               Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        super(0);
        ownerName = reactionInfo.getOwnerName();
        Fingerprint commentFingerprint = Fingerprints.comment(commentRevisionInfo.getSignatureVersion())
                .create(commentInfo, commentRevisionInfo, commentMediaDigest, postingInfo, postingRevisionInfo,
                        postingMediaDigest);
        entryFingerprint.setValue(commentFingerprint);
        negative = reactionInfo.isNegative();
        emoji = reactionInfo.getEmoji();
    }

}
