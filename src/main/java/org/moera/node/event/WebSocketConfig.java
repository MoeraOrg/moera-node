package org.moera.node.event;

import jakarta.inject.Inject;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Inject
    @Qualifier("eventScheduler")
    @Lazy
    private TaskScheduler taskScheduler;

    @Inject
    private WsHandshakeInterceptor wsHandshakeInterceptor;

    @Inject
    @Lazy
    private EventManager eventManager;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/moera/api/events")
            .setAllowedOrigins("*")
            .addInterceptors(wsHandshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue").setTaskScheduler(taskScheduler);
        registry.setPreservePublishOrder(true);
        // Start replay after the broker has registered the translated /user/queue subscription.
        registry.configureBrokerChannel().interceptors(new ExecutorChannelInterceptor() {
            @Override
            public void afterMessageHandled(
                Message<?> message,
                MessageChannel channel,
                MessageHandler handler,
                Exception ex
            ) {
                if (
                    ex == null && handler instanceof SimpleBrokerMessageHandler
                    && SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) == SimpMessageType.SUBSCRIBE
                ) {
                    eventManager.subscribed(StompHeaderAccessor.wrap(message));
                }
            }
        });
    }

    @Bean
    SmartInitializingSingleton configureEventStompEncoder(
        @Qualifier("subProtocolWebSocketHandler") WebSocketHandler handler
    ) {
        // Spring's simple broker builds the CONNECTED frame after the outbound channel interceptors have run.
        return () -> {
            for (var protocol : ((SubProtocolWebSocketHandler) handler).getProtocolHandlers()) {
                if (protocol instanceof StompSubProtocolHandler stomp) {
                    stomp.setEncoder(new EventStompEncoder(eventManager));
                }
            }
        };
    }

}
