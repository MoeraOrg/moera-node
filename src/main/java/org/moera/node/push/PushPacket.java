package org.moera.node.push;

import org.moera.node.data.PushNotification;
import org.moera.node.sse.SsePacket;

public class PushPacket extends SsePacket {

    public PushPacket(long moment, String content) {
        super(moment, content);
    }

    public PushPacket(PushNotification pushNotification) {
        this(pushNotification.getMoment(), pushNotification.getContent());
    }

}
