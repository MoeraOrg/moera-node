package org.moera.node.fingerprint;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.notification.NotificationPacket;

@FingerprintVersion(objectType = FingerprintObjectType.NOTIFICATION_PACKET, version = 0)
public class NotificationPacketFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.NOTIFICATION_PACKET.name();
    public String id;
    public String nodeName;
    public Long createdAt;
    public String type;
    public String notification;

    public NotificationPacketFingerprint(NotificationPacket packet) {
        super(0);
        id = packet.getId();
        nodeName = packet.getNodeName();
        createdAt = packet.getCreatedAt();
        type = packet.getType();
        notification = packet.getNotification();
    }

}
