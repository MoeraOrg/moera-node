package org.moera.node.liberin;

import javax.inject.Inject;

import org.moera.node.event.EventManager;
import org.moera.node.mail.Mail;
import org.moera.node.mail.MailService;
import org.moera.node.model.event.Event;
import org.moera.node.model.notification.Notification;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.NotificationSenderPool;

public abstract class LiberinReceptorBase {

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private MailService mailService;

    protected void send(Liberin liberin, Event event) {
        eventManager.send(liberin.getNodeId(), liberin.getClientId(), event);
    }

    protected void send(Direction direction, Notification notification) {
        notificationSenderPool.send(direction, notification);
    }

    public void send(Liberin liberin, Mail mail) {
        mailService.send(liberin.getNodeId(), mail);
    }

}
