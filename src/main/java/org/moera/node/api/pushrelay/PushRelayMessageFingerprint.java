package org.moera.node.api.pushrelay;

import org.moera.commons.crypto.Fingerprint;

public class PushRelayMessageFingerprint extends Fingerprint {

    public final String objectType = "PUSH_RELAY_MESSAGE";
    public long signedAt;

    public PushRelayMessageFingerprint(long signedAt) {
        super(0);
        this.signedAt = signedAt;
    }

}
