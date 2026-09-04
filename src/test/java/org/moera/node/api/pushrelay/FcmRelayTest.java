package org.moera.node.api.pushrelay;

import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.FeedWithStatus;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.PushContentType;
import org.moera.lib.pushrelay.PushRelay;
import org.moera.node.domain.Domains;
import org.moera.node.option.Options;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmRelayTest {

    private static final UUID NODE_ID = UUID.fromString("a5c1f1cd-bc83-48c5-9179-5af2f3075a24");
    private static final UUID OTHER_NODE_ID = UUID.fromString("946dd906-4140-4f25-b6e1-3fa0dfb65aba");

    @Mock
    private PushRelay service;

    @Mock
    private Domains domains;

    @Mock
    private Options options;

    private FcmRelay fcmRelay;

    @BeforeEach
    void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        fcmRelay = new FcmRelay();
        ReflectionTestUtils.setField(fcmRelay, "service", service);
        ReflectionTestUtils.setField(fcmRelay, "domains", domains);
        when(domains.getDomainOptions(any(UUID.class))).thenReturn(options);
        when(options.nodeName()).thenReturn("test-node");
        when(options.getPrivateKey("profile.signing-key"))
            .thenReturn(KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate());
    }

    @Test
    void identicalFeedStatusIsDeliveredOnlyOnce() {
        deliver(
            new Delivery(NODE_ID, feedUpdated("news", 3, 1000L)),
            new Delivery(NODE_ID, feedUpdated("news", 3, 1000L))
        );

        verify(service).feedStatus(
            eq("news"), eq(3), eq(1000L), eq("test-node"), anyLong(), any(byte[].class)
        );
    }

    @Test
    void changedNotViewedIsDelivered() {
        deliver(
            new Delivery(NODE_ID, feedUpdated("news", 3, 1000L)),
            new Delivery(NODE_ID, feedUpdated("news", 4, 1000L))
        );

        verifyFeedStatusCalls(2);
    }

    @Test
    void changedNotViewedMomentIsDelivered() {
        deliver(
            new Delivery(NODE_ID, feedUpdated("news", 3, 1000L)),
            new Delivery(NODE_ID, feedUpdated("news", 3, 1001L))
        );

        verifyFeedStatusCalls(2);
    }

    @Test
    void identicalNumbersForDifferentFeedsAreDelivered() {
        deliver(
            new Delivery(NODE_ID, feedUpdated("news", 3, 1000L)),
            new Delivery(NODE_ID, feedUpdated("explore", 3, 1000L))
        );

        verifyFeedStatusCalls(2);
    }

    @Test
    void identicalFeedStatusForDifferentNodesIsDelivered() {
        deliver(
            new Delivery(NODE_ID, feedUpdated("news", 3, 1000L)),
            new Delivery(OTHER_NODE_ID, feedUpdated("news", 3, 1000L))
        );

        verifyFeedStatusCalls(2);
    }

    private void deliver(Delivery... deliveries) {
        for (Delivery delivery : deliveries) {
            Boolean duplicate = ReflectionTestUtils.invokeMethod(
                fcmRelay, "isDuplicate", delivery.nodeId(), delivery.pushContent()
            );
            if (!Boolean.TRUE.equals(duplicate)) {
                ReflectionTestUtils.invokeMethod(fcmRelay, "deliver", delivery.nodeId(), delivery.pushContent());
            }
        }
    }

    private void verifyFeedStatusCalls(int count) {
        verify(service, times(count)).feedStatus(
            anyString(), anyInt(), anyLong(), anyString(), anyLong(), any(byte[].class)
        );
    }

    private static PushContent feedUpdated(String feedName, int notViewed, long notViewedMoment) {
        FeedWithStatus feedStatus = new FeedWithStatus();
        feedStatus.setFeedName(feedName);
        feedStatus.setNotViewed(notViewed);
        feedStatus.setNotViewedMoment(notViewedMoment);
        PushContent content = new PushContent();
        content.setType(PushContentType.FEED_UPDATED);
        content.setFeedStatus(feedStatus);
        return content;
    }

    private record Delivery(
        UUID nodeId,
        PushContent pushContent
    ) {
    }

}
