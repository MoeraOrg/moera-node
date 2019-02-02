package org.moera.node.global;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class NetworkLatencyInterceptor extends HandlerInterceptorAdapter {

    @Value("${node.mock-network-latency}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
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
