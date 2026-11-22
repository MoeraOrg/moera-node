package org.moera.node.event;

import java.util.Map;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

class EventStompEncoder extends StompEncoder {

    private final EventManager eventManager;

    EventStompEncoder(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public byte[] encode(Map<String, Object> headers, byte[] payload) {
        if (StompHeaderAccessor.getCommand(headers) == StompCommand.CONNECTED) {
            StompHeaderAccessor connected = StompHeaderAccessor.create(StompCommand.CONNECTED);
            connected.copyHeaders(headers);
            connected.setNativeHeader(SeenHeader.HEADER_NAME, eventManager.currentSeen());
            return super.encode(connected.getMessageHeaders(), payload);
        }
        return super.encode(headers, payload);
    }

}
