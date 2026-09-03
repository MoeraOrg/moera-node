package org.moera.node.global;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.bucket4j.Bucket;
import org.moera.node.model.TooManyRequestsFailure;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<BucketKey, BucketEntry> buckets = new HashMap<>();
    private final Clock clock = Clock.systemUTC();

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Class<?> controllerType = handlerMethod.getBeanType();
        Method methodType = handlerMethod.getMethod();
        RateLimit[] rateLimits = methodType.getAnnotationsByType(RateLimit.class);
        if (rateLimits.length == 0) {
            return true;
        }

        boolean providerApi = AnnotatedElementUtils.hasAnnotation(controllerType, ProviderApi.class)
            || AnnotatedElementUtils.hasAnnotation(methodType, ProviderApi.class);
        UUID nodeId = providerApi ? null : requestContext.nodeId();

        synchronized (buckets) {
            Instant now = Instant.now(clock);

            List<LimitBucket> limitBuckets = getLimitBuckets(
                rateLimits, nodeId, controllerType, methodType, false, true, now
            );
            LimitExceeded exceeded = findExceeded(limitBuckets);

            limitBuckets.addAll(getLimitBuckets(
                rateLimits, nodeId, controllerType, methodType, true, exceeded == null, now
            ));
            LimitExceeded perIpExceeded = findExceeded(limitBuckets);

            if (
                perIpExceeded != null
                && (exceeded == null || perIpExceeded.retryAfterNanos() > exceeded.retryAfterNanos())
            ) {
                exceeded = perIpExceeded;
            }

            if (exceeded != null) {
                long retryAfter = (exceeded.retryAfterNanos() + 999999999) / 1000000000;
                throw new TooManyRequestsFailure(
                    exceeded.rateLimit().limit(), exceeded.rateLimit().period(), retryAfter
                );
            }

            for (LimitBucket limitBucket : limitBuckets) {
                limitBucket.bucket().tryConsume(1);
            }
        }

        return true;
    }

    private List<LimitBucket> getLimitBuckets(
        RateLimit[] rateLimits,
        UUID nodeId,
        Class<?> controllerType,
        Method methodType,
        boolean perIp,
        boolean create,
        Instant now
    ) {
        List<LimitBucket> limitBuckets = new ArrayList<>();
        for (int i = 0; i < rateLimits.length; i++) {
            RateLimit rateLimit = rateLimits[i];
            if (rateLimit.perIp() != perIp) {
                continue;
            }
            String ip = perIp ? requestContext.getRemoteAddr().getHostAddress() : null;
            BucketKey key = new BucketKey(nodeId, controllerType, methodType, i, ip);
            BucketEntry entry = create
                ? buckets.computeIfAbsent(key, k -> buildEntry(rateLimit, now))
                : buckets.get(key);
            if (entry != null) {
                entry.setAccessedAt(now);
                limitBuckets.add(new LimitBucket(rateLimit, entry.bucket()));
            }
        }
        return limitBuckets;
    }

    private BucketEntry buildEntry(RateLimit rateLimit, Instant now) {
        Bucket bucket = Bucket.builder()
            .addLimit(limit ->
                limit
                    .capacity(rateLimit.limit())
                    .refillGreedy(rateLimit.limit(), Duration.ofSeconds(rateLimit.period()))
            )
            .build();
        return new BucketEntry(bucket, rateLimit.period(), now);
    }

    private LimitExceeded findExceeded(List<LimitBucket> limitBuckets) {
        LimitExceeded exceeded = null;
        for (LimitBucket limitBucket : limitBuckets) {
            if (limitBucket.bucket().getAvailableTokens() < 1) {
                long nanos = limitBucket.bucket().estimateAbilityToConsume(1).getNanosToWaitForRefill();
                if (exceeded == null || nanos > exceeded.retryAfterNanos()) {
                    exceeded = new LimitExceeded(limitBucket.rateLimit(), nanos);
                }
            }
        }
        return exceeded;
    }

    @Scheduled(fixedDelayString = "PT1H")
    void cleanupExpiredBuckets() {
        Instant now = Instant.now(clock);
        synchronized (buckets) {
            buckets.values().removeIf(bucket -> bucket.isExpired(now));
        }
    }

    private record BucketKey(UUID nodeId, Class<?> controllerType, Method methodType, int index, String ip) {
    }

    private static class BucketEntry {

        private final Bucket bucket;
        private final int period;
        private Instant accessedAt;

        BucketEntry(Bucket bucket, int period, Instant accessedAt) {
            this.bucket = bucket;
            this.period = period;
            this.accessedAt = accessedAt;
        }

        Bucket bucket() {
            return bucket;
        }

        void setAccessedAt(Instant accessedAt) {
            this.accessedAt = accessedAt;
        }

        boolean isExpired(Instant now) {
            return !accessedAt.plusSeconds(period).isAfter(now);
        }

    }

    private record LimitBucket(RateLimit rateLimit, Bucket bucket) {
    }

    private record LimitExceeded(RateLimit rateLimit, long retryAfterNanos) {
    }

}
