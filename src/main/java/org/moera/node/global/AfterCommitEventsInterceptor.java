package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.event.EventManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class AfterCommitEventsInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private RequestContext requestContext;

    @Inject
    private EventManager eventManager;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (ex == null) {
            requestContext.getAfterCommitEvents().forEach(
                    event -> eventManager.send(requestContext.nodeId(), requestContext.getClientId(), event)
            );
        }
    }

}
