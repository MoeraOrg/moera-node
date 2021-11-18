package org.moera.node.fingerprint;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryAttachment;

public class AttachmentFingerprintFactory extends FingerprintFactory {

    public AttachmentFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(byte[] digest) {
        var constructor = getConstructor(byte[].class);
        return constructor != null ? create(constructor, digest) : null;
    }

    public Fingerprint create(EntryAttachment attachment) {
        var constructor = getConstructor(EntryAttachment.class);
        return constructor != null ? create(constructor, attachment) : null;
    }

}
