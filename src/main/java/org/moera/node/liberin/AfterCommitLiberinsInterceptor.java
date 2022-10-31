package org.moera.node.liberin;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.NoClientId;
import org.moera.node.global.RequestContext;
import org.moera.node.subscriptions.SubscriptionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AfterCommitLiberinsInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private SubscriptionManager subscriptionManager;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (ex == null) {
            boolean noClientId = handler instanceof HandlerMethod
                    && ((HandlerMethod) handler).hasMethodAnnotation(NoClientId.class);
            String clientId = noClientId ? null : requestContext.getClientId();
            requestContext.getAfterCommitLiberins().forEach(liberin -> {
                liberin.setNodeId(requestContext.nodeId());
                liberin.setClientId(clientId);
                liberin.setPluginContext(requestContext);
            });
            liberinManager.send(requestContext.getAfterCommitLiberins());
            if (requestContext.isSubscriptionsUpdated()) {
                subscriptionManager.rescan();
            }
        }
    }

}
