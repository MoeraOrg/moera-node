package org.moera.node.global;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
import org.moera.node.global.RequestContext.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SlowRequestsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestsInterceptor.class);

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
        if (!config.getDebug().isLogSlowRequests()) {
            return;
        }

        Instant now = Instant.now();
        Instant receivedAt = requestContext.getTimes(Times.RECEIVED);
        Instant startedAt = requestContext.getTimes(Times.STARTED);
        Instant finishedAt = requestContext.getTimes(Times.FINISHED);

        long fullDuration = receivedAt.until(now, ChronoUnit.MILLIS);
        if (fullDuration < config.getDebug().getSlowRequestDuration()) {
            return;
        }
        long initDuration = receivedAt.until(startedAt, ChronoUnit.MILLIS);
        long runDuration = startedAt.until(finishedAt, ChronoUnit.MILLIS);
        long doneDuration = finishedAt.until(now, ChronoUnit.MILLIS);

        log.warn("Slow request: {}ms ({}ms..{}ms..{}ms) {} {}",
                fullDuration, initDuration, runDuration, doneDuration, request.getMethod().toUpperCase(),
                requestContext.getUrl());
    }

}
