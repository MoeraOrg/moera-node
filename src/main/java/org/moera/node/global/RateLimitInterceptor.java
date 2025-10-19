package org.moera.node.global;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.bucket4j.Bucket;
import org.moera.node.model.TooManyRequestsFailure;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Class<?> controllerType = ((HandlerMethod) handler).getBeanType();
        Method methodType = ((HandlerMethod) handler).getMethod();
        RateLimit rateLimit = AnnotatedElementUtils.findMergedAnnotation(methodType, RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = requestContext.nodeId() + "." + controllerType.getName() + "." + methodType.getName();
        Bucket bucket = buckets.computeIfAbsent(
            key,
            k -> Bucket.builder()
                .addLimit(limit ->
                    limit
                        .capacity(rateLimit.limit())
                        .refillGreedy(1, Duration.ofSeconds(rateLimit.period() / rateLimit.limit()))
                )
                .build()
        );

        if (!bucket.tryConsume(1)) {
            long retryAfter = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1000000000 + 1;
            throw new TooManyRequestsFailure(rateLimit.limit(), rateLimit.period(), retryAfter);
        }

        return true;
    }

}
