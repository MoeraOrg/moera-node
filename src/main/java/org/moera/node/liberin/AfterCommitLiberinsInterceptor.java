package org.moera.node.liberin;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.global.NoClientId;
import org.moera.node.global.RequestContext;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.subscriptions.SubscriptionManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AfterCommitLiberinsInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Inject
    @Lazy
    private LiberinManager liberinManager;

    @Inject
    @Lazy
    private SubscriptionManager subscriptionManager;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Inject
    @Lazy
    private BlockedUserOperations blockedUserOperations;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (ex == null) {
            requestContext.getFriendCacheInvalidations().forEach(friendCache::invalidate);
            requestContext.getSubscribedCacheInvalidations().forEach(subscribedCache::invalidate);
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
            if (requestContext.isBlockedUsersUpdated()) {
                blockedUserOperations.recalculateChecksums(requestContext.nodeId());
            }
        }
    }

}
