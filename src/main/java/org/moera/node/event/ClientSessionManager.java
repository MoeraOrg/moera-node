package org.moera.node.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Service
public class ClientSessionManager {

    private static Logger log = LoggerFactory.getLogger(ClientSessionManager.class);

    @EventListener(SessionConnectedEvent.class)
    public void sessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("Session connected, id = {}", accessor.getSessionId());
    }

}
