package org.moera.node.api.pushrelay;

import org.moera.commons.crypto.Fingerprint;

public class PushRelayRegisterFingerprint extends Fingerprint {

    public String clientId;
    public String nodeName;
    public String lang;

    public PushRelayRegisterFingerprint(String clientId, String nodeName, String lang) {
        super(0);
        this.clientId = clientId;
        this.nodeName = nodeName;
        this.lang = lang;
    }

}
