package org.moera.node.event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.notification.send.NotificationSenderPool;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class AfterCommitEventsInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private RequestContext requestContext;

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (ex == null) {
            requestContext.getAfterCommitEvents().forEach(
                    event -> eventManager.send(requestContext.nodeId(), requestContext.getClientId(), event)
            );
            requestContext.getAfterCommitNotifications().forEach(notificationSenderPool::send);
        }
    }

}
