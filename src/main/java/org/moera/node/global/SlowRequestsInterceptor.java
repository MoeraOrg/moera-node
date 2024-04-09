package org.moera.node.global;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
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
        requestContext.setStartedAt(Instant.now());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (!config.getDebug().isLogSlowRequests()) {
            return;
        }
        long duration = requestContext.getStartedAt().until(Instant.now(), ChronoUnit.MILLIS);
        if (duration < config.getDebug().getSlowRequestDuration()) {
            return;
        }
        log.warn("Slow request: {}ms {} {}", duration, request.getMethod().toUpperCase(), requestContext.getUrl());
    }

}
