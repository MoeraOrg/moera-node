package org.moera.node.fingerprint;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.notification.NotificationPacket;

public class NotificationPacketFingerprintFactory extends FingerprintFactory {

    public NotificationPacketFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(NotificationPacket packet) {
        var constructor = getConstructor(NotificationPacket.class);
        return constructor != null ? create(constructor, packet) : null;
    }

}
