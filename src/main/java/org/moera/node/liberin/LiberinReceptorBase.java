package org.moera.node.liberin;

import javax.inject.Inject;

import org.moera.node.event.EventManager;
import org.moera.node.mail.Mail;
import org.moera.node.mail.MailService;
import org.moera.node.model.event.Event;
import org.moera.node.model.notification.Notification;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.push.PushContent;
import org.moera.node.push.PushService;

public abstract class LiberinReceptorBase {

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private MailService mailService;

    @Inject
    private PushService pushService;

    protected void send(Liberin liberin, Event event) {
        eventManager.send(liberin.getNodeId(), liberin.getClientId(), event);
    }

    protected void send(Direction direction, Notification notification) {
        notificationSenderPool.send(direction, notification);
    }

    public void send(Liberin liberin, Mail mail) {
        mailService.send(liberin.getNodeId(), mail);
    }

    public void send(Liberin liberin, PushContent pushContent) {
        pushService.send(liberin.getNodeId(), pushContent);
    }

}
