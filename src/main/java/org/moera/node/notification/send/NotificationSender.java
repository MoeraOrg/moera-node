package org.moera.node.notification.send;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiValidationException;
import org.moera.node.fingerprint.NotificationPacketFingerprint;
import org.moera.node.model.Result;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.SubscriberNotification;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationSender extends Task {

    private static Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private String receiverNodeName;
    private BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private NotificationSenderPool pool;

    @Inject
    private ObjectMapper objectMapper;

    public NotificationSender(NotificationSenderPool pool, String receiverNodeName) {
        this.pool = pool;
        this.receiverNodeName = receiverNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Notification notification) throws InterruptedException {
        queue.put(notification);
    }

    @Override
    public void run() {
        while (!stopped) {
            Notification notification;
            try {
                notification = queue.poll(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                continue;
            }
            if (notification == null) {
                stopped = true;
                if (!queue.isEmpty()) { // queue may receive content before the previous statement
                    stopped = false;
                }
            } else {
                deliver(notification);
            }
        }
        pool.deleteSender(nodeId, receiverNodeName);
    }

    private void deliver(Notification notification) {
        log.info("Delivering notification {} to node '{}'", notification.getType().name(), receiverNodeName);

        nodeApi.setNodeId(nodeId);
        try {
            succeeded(nodeApi.postNotification(receiverNodeName, createPacket(notification)));
        } catch (Exception e) {
            error(e, notification);
        }
    }

    private NotificationPacket createPacket(Notification notification) {
        NotificationPacket packet = new NotificationPacket();
        packet.setId(UUID.randomUUID().toString());
        packet.setNodeName(nodeName);
        packet.setCreatedAt(Util.toEpochSecond(Util.now()));
        packet.setType(notification.getType().getValue());

        try {
            packet.setNotification(objectMapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            failed("Cannot serialize the notification object");
        }

        NotificationPacketFingerprint fingerprint = new NotificationPacketFingerprint(packet);
        packet.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        packet.setSignatureVersion(NotificationPacketFingerprint.VERSION);

        return packet;
    }

    private void succeeded(Result result) {
        initLoggingDomain();
        if (result.isOk()) {
            log.info("Notification delivered successfully");
        } else {
            log.info("Receiving node returned error: {}", result.getMessage());
        }
    }

    private void error(Throwable e, Notification notification) {
        if (e instanceof NodeApiValidationException
                && ((NodeApiValidationException) e).getErrorCode().equals("subscription.unsubscribe")
                && notification instanceof SubscriberNotification) {
            pool.unsubscribe(UUID.fromString(((SubscriberNotification) notification).getSubscriberId()));
        }
        failed(e.getMessage());
    }

    private void failed(String message) {
        initLoggingDomain();
        log.error(message);
    }

}
