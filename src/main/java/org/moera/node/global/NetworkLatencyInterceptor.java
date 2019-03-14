package org.moera.node.global;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class NetworkLatencyInterceptor extends HandlerInterceptorAdapter {

    private static Logger log = LoggerFactory.getLogger(NetworkLatencyInterceptor.class);

    @Value("${node.mock-network-latency}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled) {
            log.info("Emulation of network latency is enabled."
                    + " Random delay of 200ms up to 2s will be added to all responses");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        Class<?> controllerType = ((HandlerMethod) handler).getBeanType();
        if (!AnnotatedElementUtils.hasAnnotation(controllerType, ApiController.class)) {
            return true;
        }

        int period = Util.random(200, 2000);
        log.debug("Delayed by {}ms", period);
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
        }

        return true;
    }

}
