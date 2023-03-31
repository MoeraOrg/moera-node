package org.moera.node.fingerprint;

import java.util.Objects;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.SheriffOrderDetails;

@FingerprintVersion(objectType = FingerprintObjectType.SHERIFF_ORDER, version = 1)
public class SheriffOrderFingerprint extends Fingerprint {

    public static final short VERSION = 1;

    public String objectType = FingerprintObjectType.SHERIFF_ORDER.name();
    public String sheriffName;
    public String nodeName;
    public String feedName;
    public Digest<Fingerprint> entryFingerprint;
    public String category;
    public String reasonCode;
    public String reasonDetails;
    public long createdAt;

    public SheriffOrderFingerprint() {
        super(1);
    }

    public SheriffOrderFingerprint(String nodeName, SheriffOrderDetails sheriffOrderDetails, byte[] entryDigest) {
        super(1);
        sheriffName = sheriffOrderDetails.getSheriffName();
        this.nodeName = nodeName;
        feedName = sheriffOrderDetails.getFeedName();
        if (entryDigest != null) {
            entryFingerprint = new Digest<>();
            entryFingerprint.setDigest(entryDigest);
        }
        category = Objects.toString(sheriffOrderDetails.getCategory(), null);
        reasonCode = Objects.toString(sheriffOrderDetails.getReasonCode(), null);
        reasonDetails = sheriffOrderDetails.getReasonDetails();
        createdAt = sheriffOrderDetails.getCreatedAt();
    }

}
