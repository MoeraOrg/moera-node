package org.moera.node.fingerprint;

import org.moera.lib.crypto.FingerprintException;
import org.moera.lib.node.Fingerprints;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.util.Util;

public class NotificationPacketFingerprintBuilder {

    public static final short LATEST_VERSION = 1;

    public static byte[] build(NotificationPacket packet) {
        return build(LATEST_VERSION, packet);
    }

    public static byte[] build(short version, NotificationPacket packet) {
        return switch (version) {
            case 1 ->
                Fingerprints.notificationPacket1(
                    packet.getId(),
                    packet.getNodeName(),
                    packet.getFullName(),
                    Util.toTimestamp(packet.getCreatedAt()),
                    packet.getType(),
                    packet.getNotification()
                );
            case 0 ->
                Fingerprints.notificationPacket0(
                    packet.getId(),
                    packet.getNodeName(),
                    Util.toTimestamp(packet.getCreatedAt()),
                    packet.getType(),
                    packet.getNotification()
                );
            default -> throw new FingerprintException("Unknown fingerprint version: " + version);
        };
    }

}
