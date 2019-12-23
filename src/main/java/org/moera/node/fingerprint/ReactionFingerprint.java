package org.moera.node.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionDescription;

@FingerprintVersion(objectType = FingerprintObjectType.REACTION, version = 0)
public class ReactionFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.REACTION.name();
    public String ownerName;
    public Digest<PostingFingerprint> postingFingerprint = new Digest<>();
    public boolean negative;
    public int emoji;

    public ReactionFingerprint(ReactionDescription description, byte[] postingDigest) {
        super(0);
        ownerName = description.getOwnerName();
        postingFingerprint.setDigest(postingDigest);
        negative = description.isNegative();
        emoji = description.getEmoji();
    }

    public ReactionFingerprint(String ownerName, ReactionAttributes attributes, PostingFingerprint postingFingerprint) {
        super(0);
        this.ownerName = ownerName;
        this.postingFingerprint.setValue(postingFingerprint);
        negative = attributes.isNegative();
        emoji = attributes.getEmoji();
    }

}
