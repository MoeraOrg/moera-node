package org.moera.node.event;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

class SeenHeaderTest {

    @Test
    void parsesSeenHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setNativeHeader("seen", "123, 45");

        Assertions.assertEquals(new SeenHeader.Details(123L, 45), SeenHeader.parse(accessor));
    }

    @Test
    void ignoresInvalidSeenHeaderParts() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setNativeHeader("seen", "invalid, 45");

        Assertions.assertEquals(new SeenHeader.Details(null, 45), SeenHeader.parse(accessor));
    }

}
