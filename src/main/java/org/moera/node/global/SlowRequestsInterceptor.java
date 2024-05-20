package org.moera.node.global;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
import org.moera.node.global.RequestContext.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SlowRequestsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestsInterceptor.class);

    private final Map<String, RequestStatistics> byRequest = new ConcurrentHashMap<>();

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestContext.setTimes(Times.RECEIVED, Instant.now());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        Instant now = Instant.now();
        Instant receivedAt = requestContext.getTimes(Times.RECEIVED);
        Instant startedAt = requestContext.getTimes(Times.STARTED);
        Instant finishedAt = requestContext.getTimes(Times.FINISHED);

        long fullDuration = receivedAt.until(now, ChronoUnit.MILLIS);
        String handlerName = getHandlerName(handler);
        if (handlerName != null) {
            RequestStatistics stat = byRequest.computeIfAbsent(handlerName, k -> new RequestStatistics());
            stat.minDuration.accumulateAndGet(fullDuration, Math::min);
            stat.maxDuration.accumulateAndGet(fullDuration, Math::max);
            stat.totalDuration.addAndGet(fullDuration);
            stat.medianDuration.add(fullDuration);
            stat.count.incrementAndGet();
            if (fullDuration >= config.getDebug().getSlowRequestDuration()) {
                stat.slowCount.incrementAndGet();
            }
        }
        if (fullDuration < config.getDebug().getSlowRequestDuration()) {
            return;
        }
        long initDuration = receivedAt.until(startedAt, ChronoUnit.MILLIS);
        long runDuration = startedAt.until(finishedAt, ChronoUnit.MILLIS);

        log.warn("Slow request: {}ms ({}ms..{}ms) {} {}",
                fullDuration, initDuration, runDuration, request.getMethod().toUpperCase(), requestContext.getUrl());
    }

    private String getHandlerName(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        return String.format("%s.%s", handlerMethod.getBeanType().getSimpleName(), handlerMethod.getMethod().getName());
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void reportStatistics() {
        log.debug("Requests statistics:");
        byRequest.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String name = entry.getKey();
                    RequestStatistics stat = entry.getValue();
                    long total = stat.count.get();
                    if (total == 0) {
                        return;
                    }
                    long avg = (long) ((double) stat.totalDuration.get() / total);
                    long med = stat.medianDuration.getMedian();
                    long min = stat.minDuration.get();
                    long max = stat.maxDuration.get();
                    long slow = stat.slowCount.get();
                    String slowPercent = String.format("%.3f", (double) slow / total);
                    log.debug("{}: avg = {}ms, med = {}ms, min = {}ms, max = {}ms, total = {}, slow = {} ({}%)",
                            name, avg, med, min, max, total, slow, slowPercent);
                });
    }

}
