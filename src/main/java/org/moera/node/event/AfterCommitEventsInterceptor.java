package org.moera.node.event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.liberin.LiberinManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AfterCommitEventsInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private LiberinManager liberinManager;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (ex == null) {
            requestContext.getAfterCommitLiberins().forEach(liberin -> {
                liberin.setNodeId(requestContext.nodeId());
                liberin.setClientId(requestContext.getClientId());
            });
            liberinManager.send(requestContext.getAfterCommitLiberins());
        }
    }

}
