package org.moera.node.global;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.node.model.TooManyRequestsFailure;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

public class RateLimitInterceptorTest {

    private final TestRequestContext requestContext = new TestRequestContext();
    private final MutableClock clock = new MutableClock();

    private RateLimitInterceptor interceptor;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        interceptor = new RateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "requestContext", requestContext);
        ReflectionTestUtils.setField(interceptor, "clock", clock);
        handler = new HandlerMethod(new TestController(), TestController.class.getMethod("limited"));
    }

    @Test
    void rejectsRequestWhenPerIpLimitIsExceeded() throws Exception {
        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.1"));

        interceptor.preHandle(null, null, handler);

        TooManyRequestsFailure failure = Assertions.assertThrows(
            TooManyRequestsFailure.class,
            () -> interceptor.preHandle(null, null, handler)
        );
        Assertions.assertEquals(1, failure.getLimit());
        Assertions.assertEquals(3600, failure.getPeriod());
    }

    @Test
    void usesSeparatePerIpBucketsAndOneSharedGlobalBucket() throws Exception {
        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.1"));
        interceptor.preHandle(null, null, handler);

        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.2"));
        interceptor.preHandle(null, null, handler);

        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.3"));
        TooManyRequestsFailure failure = Assertions.assertThrows(
            TooManyRequestsFailure.class,
            () -> interceptor.preHandle(null, null, handler)
        );
        Assertions.assertEquals(2, failure.getLimit());
        Assertions.assertEquals(3600, failure.getPeriod());
    }

    @Test
    void providerLimitIsSharedBetweenNodeContexts() throws Exception {
        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.1"));
        requestContext.setNodeId(UUID.randomUUID());
        interceptor.preHandle(null, null, handler);

        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.2"));
        requestContext.setNodeId(UUID.randomUUID());
        interceptor.preHandle(null, null, handler);

        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.3"));
        requestContext.setNodeId(UUID.randomUUID());
        Assertions.assertThrows(
            TooManyRequestsFailure.class,
            () -> interceptor.preHandle(null, null, handler)
        );
    }

    @Test
    void rejectedRequestDoesNotConsumeTokensFromOtherLimits() throws Exception {
        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.1"));
        interceptor.preHandle(null, null, handler);
        Assertions.assertThrows(
            TooManyRequestsFailure.class,
            () -> interceptor.preHandle(null, null, handler)
        );

        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.2"));
        Assertions.assertDoesNotThrow(() -> interceptor.preHandle(null, null, handler));
    }

    @Test
    void removesInactiveBucketsAfterTheirLimitPeriod() throws Exception {
        HandlerMethod shortLimitHandler = new HandlerMethod(
            new TestController(), TestController.class.getMethod("shortLimit")
        );
        requestContext.setRemoteAddr(InetAddress.getByName("192.0.2.1"));
        interceptor.preHandle(null, null, shortLimitHandler);

        clock.advance(Duration.ofSeconds(1));
        interceptor.cleanupExpiredBuckets();

        Map<?, ?> buckets = (Map<?, ?>) ReflectionTestUtils.getField(interceptor, "buckets");
        Assertions.assertTrue(buckets.isEmpty());
    }

    private static class TestController {

        @ProviderApi
        @RateLimit(limit = 2, period = 3600)
        @RateLimit(limit = 1, period = 3600, perIp = true)
        public void limited() {
        }

        @RateLimit(limit = 1, period = 1, perIp = true)
        public void shortLimit() {
        }

    }

    private static class TestRequestContext extends RequestContextImpl {

        private UUID nodeId;

        @Override
        public UUID nodeId() {
            return nodeId;
        }

        public void setNodeId(UUID nodeId) {
            this.nodeId = nodeId;
        }

    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.EPOCH;

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

    }

}
