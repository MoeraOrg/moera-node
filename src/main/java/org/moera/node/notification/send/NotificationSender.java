package org.moera.node.notification.send;

import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private static final Duration RETRY_MIN_DELAY = Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration RETRY_MAX_DELAY = Duration.of(6, ChronoUnit.HOURS);
    private static final Duration RETRY_PERIOD = Duration.of(7, ChronoUnit.DAYS);
    private static final Duration SUBSCRIPTION_DELAY = Duration.of(7, ChronoUnit.MINUTES);

    private static Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private String receiverNodeName;
    private BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private Notification notification;
    private Duration delay;
    private boolean stopped = false;
    private Instant pausedTill;
    private NotificationSenderPool pool;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private PendingNotificationRepository pendingNotificationRepository;

    public NotificationSender(NotificationSenderPool pool, String receiverNodeName) {
        this.pool = pool;
        this.receiverNodeName = receiverNodeName;
    }

    public String getReceiverNodeName() {
        return receiverNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public Instant getPausedTill() {
        return pausedTill;
    }

    public void put(@NotNull Notification notification) throws InterruptedException {
        queue.put(notification);
    }

    @Override
    public void run() {
        initLoggingDomain();
        if (notification == null) {
            log.debug("Sender from node ID = {} to '{}' started", nodeId, receiverNodeName);
        } else {
            log.debug("Sender from node ID = {} to '{}' resumed", nodeId, receiverNodeName);
        }
        while (!stopped) {
            if (notification == null) {
                try {
                    notification = queue.poll(1, TimeUnit.MINUTES);
                    delay = null;
                } catch (InterruptedException e) {
                    continue;
                }
            }
            if (notification == null) {
                stopped = true;
                if (!queue.isEmpty()) { // queue may receive content before the previous statement
                    stopped = false;
                }
            } else {
                deliver(notification);
                if (pausedTill == null) {
                    notification = null;
                } else {
                    pool.pauseSender(this);
                    log.debug("Sender from node ID = {} to '{}' paused", nodeId, receiverNodeName);
                    return;
                }
            }
        }
        pool.deleteSender(nodeId, receiverNodeName);
        log.debug("Sender from node ID = {} to '{}' stopped", nodeId, receiverNodeName);
    }

    private void deliver(Notification notification) {
        nodeApi.setNodeId(nodeId);
        do {
            if (delay != null && pausedTill == null) {
                if (delay.compareTo(Duration.ofMinutes(2)) <= 0) {
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException e) {
                    }
                } else {
                    pausedTill = Instant.now().plus(delay);
                    return;
                }
            }
            pausedTill = null;

            log.info("Delivering notification {} to node '{}'", notification.getType().name(), receiverNodeName);

            try {
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

            Duration totalPeriod = Duration.between(notification.getCreatedAt().toInstant(), Instant.now());
            if (totalPeriod.compareTo(RETRY_PERIOD) < 0) {
                delay = delay == null ? RETRY_MIN_DELAY : delay.multipliedBy(2);
                delay = delay.compareTo(RETRY_MAX_DELAY) < 0 ? delay : RETRY_MAX_DELAY;
                log.info("Notification delivery failed, retry in {}s", delay.toSeconds());
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
        packet.setNotification(objectMapper.writeValueAsString(notification));

        NotificationPacketFingerprint fingerprint = new NotificationPacketFingerprint(packet);
        packet.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        packet.setSignatureVersion(NotificationPacketFingerprint.VERSION);

        return packet;
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
            SubscriberNotification sn = (SubscriberNotification) notification;
            if (sn.getSubscriptionCreatedAt() != null
                    && sn.getSubscriptionCreatedAt().toInstant().plus(SUBSCRIPTION_DELAY).isAfter(Instant.now())) {
                fatal = false;
            } else {
                pool.unsubscribe(UUID.fromString(sn.getSubscriberId()));
                fatal = true;
            }
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
