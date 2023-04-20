package org.moera.node.fingerprint;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.SheriffOrderDetails;
import org.moera.node.model.SheriffOrderDetailsQ;

public class SheriffOrderFingerprintFactory extends FingerprintFactory {

    public SheriffOrderFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(ReactionDescription description, byte[] entryDigest) {
        var constructor = getConstructor(ReactionDescription.class, byte[].class);
        return constructor != null ? create(constructor, description, entryDigest) : null;
    }

    public Fingerprint create(String nodeName, SheriffOrderDetailsQ sheriffOrderDetails, byte[] entryDigest) {
        var constructor = getConstructor(String.class, SheriffOrderDetailsQ.class, byte[].class);
        return constructor != null ? create(constructor, nodeName, sheriffOrderDetails, entryDigest) : null;
    }

    public Fingerprint create(String nodeName, SheriffOrderDetails sheriffOrderDetails, byte[] entryDigest) {
        var constructor = getConstructor(String.class, SheriffOrderDetails.class, byte[].class);
        return constructor != null ? create(constructor, nodeName, sheriffOrderDetails, entryDigest) : null;
    }

}
