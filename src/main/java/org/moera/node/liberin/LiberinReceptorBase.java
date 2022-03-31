package org.moera.node.liberin;

import javax.inject.Inject;

import org.moera.node.event.EventManager;
import org.moera.node.mail.MailService;
import org.moera.node.notification.send.NotificationSenderPool;

public abstract class LiberinReceptorBase {

    @Inject
    protected EventManager eventManager;

    @Inject
    protected NotificationSenderPool notificationSenderPool;

    @Inject
    protected MailService mailService;

}
