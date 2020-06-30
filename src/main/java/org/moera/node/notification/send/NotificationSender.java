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
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.api.NodeApiValidationException;
import org.moera.node.data.PendingNotification;
import org.moera.node.data.PendingNotificationRepository;
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

    private static final long RETRY_MIN_DELAY = 30;
    private static final long RETRY_MAX_DELAY = 6 * 60 * 60;
    private static final long RETRY_PERIOD = 7 * 24 * 60 * 60;

    private static Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private String receiverNodeName;
    private BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private NotificationSenderPool pool;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private PendingNotificationRepository pendingNotificationRepository;

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
        initLoggingDomain();
        nodeApi.setNodeId(nodeId);

        long delay = 0;
        do {
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
            }

            log.info("Delivering notification {} to node '{}'", notification.getType().name(), receiverNodeName);

            try {
                storePending(notification);
                Result result = nodeApi.postNotification(receiverNodeName, createPacket(notification));
                succeeded(result);
                break;
            } catch (Throwable e) {
                boolean fatal = error(e, notification);
                if (fatal) {
                    log.info("Notification delivery failed fatally");
                    break;
                }
            }

            long totalPeriod = Util.toEpochSecond(Util.now()) - Util.toEpochSecond(notification.getCreatedAt());
            if (totalPeriod < RETRY_PERIOD) {
                delay = delay == 0 ? RETRY_MIN_DELAY : delay * 2;
                delay = Math.min(delay, RETRY_MAX_DELAY);
                log.info("Notification delivery failed, retry in {}s", delay);
            } else {
                log.info("Notification delivery failed, giving up");
                break;
            }
        } while (true);
        deletePending(notification);
    }

    private NotificationPacket createPacket(Notification notification) throws JsonProcessingException {
        NotificationPacket packet = new NotificationPacket();
        packet.setId(UUID.randomUUID().toString());
        packet.setNodeName(nodeName);
        packet.setCreatedAt(Util.toEpochSecond(Util.now()));
        packet.setType(notification.getType().getValue());
        packet.setNotification(serialize(notification));

        NotificationPacketFingerprint fingerprint = new NotificationPacketFingerprint(packet);
        packet.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        packet.setSignatureVersion(NotificationPacketFingerprint.VERSION);

        return packet;
    }

    private String serialize(Notification notification) throws JsonProcessingException {
        return objectMapper.writeValueAsString(notification);
    }

    private void storePending(Notification notification) throws JsonProcessingException {
        if (notification.getPendingNotificationId() != null) {
            return;
        }
        PendingNotification pending = new PendingNotification();
        pending.setId(UUID.randomUUID());
        pending.setNodeId(nodeId);
        pending.setNodeName(receiverNodeName);
        pending.setNotification(serialize(notification));
        inTransactionQuietly(() -> {
            pendingNotificationRepository.save(pending);
            notification.setPendingNotificationId(pending.getId());
            return null;
        });
    }

    private void deletePending(Notification notification) {
        if (notification.getPendingNotificationId() == null) {
            return;
        }
        inTransactionQuietly(() -> {
            pendingNotificationRepository.deleteById(notification.getPendingNotificationId());
            return null;
        });
    }

    private void succeeded(Result result) {
        initLoggingDomain();
        if (result.isOk()) {
            log.info("Notification delivered successfully");
        } else {
            log.info("Receiving node returned error: {}", result.getMessage());
        }
    }

    private boolean error(Throwable e, Notification notification) {
        boolean fatal = false;

        if (e instanceof NodeApiValidationException
                && ((NodeApiValidationException) e).getErrorCode().equals("subscription.unsubscribe")
                && notification instanceof SubscriberNotification) {
            pool.unsubscribe(UUID.fromString(((SubscriberNotification) notification).getSubscriberId()));
            fatal = true;
        } else if (e instanceof NodeApiNotFoundException || e instanceof JsonProcessingException) {
            fatal = true;
        }
        failed(e.getMessage());

        return fatal;
    }

    private void failed(String message) {
        initLoggingDomain();
        log.error(message);
    }

}
