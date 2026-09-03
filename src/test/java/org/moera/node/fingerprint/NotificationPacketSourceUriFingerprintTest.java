package org.moera.node.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.NotificationPacket;

class NotificationPacketSourceUriFingerprintTest {

    @Test
    void sourceUriIsProtectedStartingWithVersionTwo() {
        NotificationPacket first = packet("https://source.example/first");
        NotificationPacket second = packet("https://source.example/second");

        assertThat(NotificationPacketFingerprintBuilder.build((short) 1, first))
            .isEqualTo(NotificationPacketFingerprintBuilder.build((short) 1, second));
        assertThat(NotificationPacketFingerprintBuilder.build((short) 2, first))
            .isNotEqualTo(NotificationPacketFingerprintBuilder.build((short) 2, second));
        assertThat(NotificationPacketFingerprintBuilder.LATEST_VERSION).isEqualTo((short) 2);
    }

    private static NotificationPacket packet(String sourceUri) {
        var packet = new NotificationPacket();
        packet.setId("packet-id");
        packet.setNodeName("node.example");
        packet.setFullName("Node");
        packet.setNodeSourceUri(sourceUri);
        packet.setCreatedAt(123L);
        packet.setType("test");
        packet.setNotification("{}");
        return packet;
    }

}
