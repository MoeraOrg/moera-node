package org.moera.node.fingerprint;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryAttachment;

@FingerprintVersion(objectType = FingerprintObjectType.ATTACHMENT, version = 0)
public class AttachmentFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.ATTACHMENT.name();
    public byte[] digest;

    public AttachmentFingerprint(byte[] digest) {
        super(0);
        this.digest = digest;
    }

    public AttachmentFingerprint(EntryAttachment attachment) {
        super(0);
        digest = attachment.getMediaFileOwner().getMediaFile().getDigest();
    }

}
