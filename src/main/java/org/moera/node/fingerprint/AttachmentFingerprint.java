package org.moera.node.fingerprint;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFileOwner;

@FingerprintVersion(objectType = FingerprintObjectType.ATTACHMENT, version = 0)
@Deprecated
public class AttachmentFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.ATTACHMENT.name();
    public byte[] digest;

    public AttachmentFingerprint(byte[] digest) {
        super(0);
        this.digest = digest;
    }

    public AttachmentFingerprint(MediaFileOwner mediaFileOwner) {
        this(mediaFileOwner.getMediaFile().getDigest());
    }

    public AttachmentFingerprint(EntryAttachment attachment) {
        this(attachment.getMediaFileOwner());
    }

}
