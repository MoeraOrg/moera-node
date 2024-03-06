package org.moera.node.api.pushrelay;

import org.moera.commons.crypto.Fingerprint;

public class PushRelayRegisterFingerprint extends Fingerprint {

    public final String objectType = "PUSH_RELAY_REGISTER";
    public String clientId;
    public String lang;
    public long signedAt;

    public PushRelayRegisterFingerprint(String clientId, String lang, long signedAt) {
        super(0);
        this.clientId = clientId;
        this.lang = lang;
        this.signedAt = signedAt;
    }

}
