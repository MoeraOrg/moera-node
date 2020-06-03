package org.moera.node.notification.send;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.model.notification.Notification;
import org.moera.node.task.TaskAutowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class NotificationSenderPool {

    private ConcurrentMap<Direction, NotificationSender> senders = new ConcurrentHashMap<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    @Qualifier("notificationSenderTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    public void send(Notification notification) {
        Direction direction = new Direction(requestContext.nodeId(), notification.getReceiverNodeName());
        while (true) {
            NotificationSender sender;
            do {
                sender = senders.computeIfAbsent(direction, d -> createSender(d.getNodeName()));
            } while (sender.isStopped());
            try {
                sender.put(notification);
            } catch (InterruptedException e) {
                continue;
            }
            break;
        }
    }

    private NotificationSender createSender(String nodeName) {
        NotificationSender sender = new NotificationSender(this, nodeName);
        taskAutowire.autowire(sender);
        taskExecutor.execute(sender);
        return sender;
    }

    void deleteSender(UUID nodeId, String nodeName) {
        senders.remove(new Direction(nodeId, nodeName));
    }

}
