package org.moera.node.liberin;

import jakarta.inject.Inject;

import org.moera.node.api.pushrelay.FcmRelay;
import org.moera.node.event.EventManager;
import org.moera.node.global.UniversalContext;
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
    protected UniversalContext universalContext;

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private MailService mailService;

    @Inject
    private PushService pushService;

    @Inject
    private FcmRelay fcmRelay;

    protected void send(Liberin subLiberin) {
        universalContext.send(subLiberin);
    }

    protected void send(Liberin liberin, Event event) {
        eventManager.send(liberin.getNodeId(), liberin.getClientId(), event);
    }

    protected void send(Direction direction, Notification notification) {
        notificationSenderPool.send(direction, notification);
    }

    protected void send(Mail mail) {
        mailService.send(universalContext.nodeId(), mail);
    }

    protected void sendToRoot(Mail mail) {
        mailService.sendToRoot(universalContext.nodeId(), mail);
    }

    protected void send(PushContent pushContent) {
        pushService.send(universalContext.nodeId(), pushContent);
        fcmRelay.send(universalContext.nodeId(), pushContent);
    }

}
