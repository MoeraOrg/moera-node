package org.moera.node.global;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class NetworkLatencyInterceptor extends HandlerInterceptorAdapter {

    @Value("${node.mock-network-latency}")
    private boolean enabled;

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
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
        }

        return true;
    }

}
