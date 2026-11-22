package org.moera.node.event;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.node.auth.AuthenticationManager;
import org.moera.node.domain.Domains;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.event.PingEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventConnectionTest {

    private final UUID nodeId = UUID.randomUUID();
    private final EventManager events = new EventManager();
    private final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final Queue<Message<byte[]>> frames = new ArrayDeque<>();
    private WebSocketHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        Domains domains = mock(Domains.class);
        when(domains.getDomainNodeId("test")).thenReturn(nodeId);
        ReflectionTestUtils.setField(events, "domains", domains);
        ReflectionTestUtils.setField(events, "universalContext", mock(UniversalContext.class));
        ReflectionTestUtils.setField(events, "authenticationManager", mock(AuthenticationManager.class));
        context.setServletContext(new MockServletContext());
        context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("eventManager", events));
        context.register(BrokerConfig.class);
        context.refresh();
        ReflectionTestUtils.setField(events, "messagingTemplate", context.getBean(SimpMessagingTemplate.class));
        handler = context.getBean("subProtocolWebSocketHandler", WebSocketHandler.class);
        when(session.getId()).thenReturn("session");
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);
        StompDecoder decoder = new StompDecoder();
        doAnswer(invocation -> {
            TextMessage message = invocation.getArgument(0);
            frames.addAll(decoder.decode(ByteBuffer.wrap(message.asBytes())));
            return null;
        }).when(session).sendMessage(any(TextMessage.class));
        handler.afterConnectionEstablished(session);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void advertisesQueueTailAndReplaysAnEventBetweenConnectAndSubscribe() throws Exception {
        events.send(nodeId, new PingEvent());
        StompHeaderAccessor connected = connect();
        String seen = connected.getFirstNativeHeader("seen");
        assertNotNull(seen);
        assertTrue(seen.matches("[0-9]+,1"));
        assertEquals("1.2", connected.getVersion());

        events.send(nodeId, new PingEvent());
        subscribe(seen);
        JsonNode packet = receivePacket();
        assertEquals("PING", packet.at("/event/type").asString());
        assertEquals(2, packet.get("ordinal").asInt());
        assertTrue(frames.isEmpty());
    }

    @Test
    void reportsAQueueRestartEvenWhileTheNewQueueIsEmpty() throws Exception {
        connect();
        subscribe("1,123");
        JsonNode packet = receivePacket();
        assertEquals("WAKE_UP", packet.at("/event/type").asString());
        assertTrue(packet.get("queueStartedAt").asLong() > 1);
        assertEquals(0, packet.get("ordinal").asInt());
        assertTrue(frames.isEmpty());
    }

    @Test
    void startsAtTheCurrentTailForAClientWithoutSeen() throws Exception {
        connect();
        events.send(nodeId, new PingEvent());
        subscribe(null);
        events.send(nodeId, new PingEvent());
        JsonNode packet = receivePacket();
        assertEquals("PING", packet.at("/event/type").asString());
        assertEquals(2, packet.get("ordinal").asInt());
        assertTrue(frames.isEmpty());
    }

    private StompHeaderAccessor connect() throws Exception {
        send("CONNECT\naccept-version:1.2\nhost:test\nheart-beat:0,0\n\n");
        return StompHeaderAccessor.wrap(frames.remove());
    }

    private void subscribe(String seen) throws Exception {
        String header = seen != null ? "seen:" + seen + "\n" : "";
        send("SUBSCRIBE\nid:sub-0\ndestination:/user/queue\n" + header + "\n");
    }

    private void send(String frame) throws Exception {
        handler.handleMessage(session, new TextMessage(frame + '\0'));
    }

    private JsonNode receivePacket() {
        ReflectionTestUtils.invokeMethod(events, "deliver");
        Message<byte[]> frame = frames.poll();
        assertNotNull(frame, "Expected an event on the subscribed WebSocket");
        return new JsonMapper().readTree(frame.getPayload());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({WebSocketConfig.class, WsHandshakeInterceptor.class})
    static class BrokerConfig implements WebSocketMessageBrokerConfigurer {

        @Bean
        ThreadPoolTaskScheduler eventScheduler() {
            return new ThreadPoolTaskScheduler();
        }

        @Override
        public void configureClientInboundChannel(ChannelRegistration registration) {
            registration.executor(new SyncTaskExecutor());
        }

        @Override
        public void configureClientOutboundChannel(ChannelRegistration registration) {
            registration.executor(new SyncTaskExecutor());
        }
    }
}
