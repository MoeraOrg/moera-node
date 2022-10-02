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
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.api.NodeApiValidationException;
import org.moera.node.data.PendingNotificationRepository;
import org.moera.node.fingerprint.NotificationPacketFingerprint;
import org.moera.node.model.AvatarImage;
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
    private static final Duration RETRY_NAME_PERIOD = Duration.of(3, ChronoUnit.HOURS);
    private static final Duration SUBSCRIPTION_DELAY = Duration.of(7, ChronoUnit.MINUTES);

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private final String receiverNodeName;
    private final BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private Notification notification;
    private Duration delay;
    private boolean stopped = false;
    private Instant pausedTill;
    private final NotificationSenderPool pool;

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

    public void setPausedTill(Instant pausedTill) {
        this.pausedTill = pausedTill;
    }

    public void put(@NotNull Notification notification) throws InterruptedException {
        queue.put(notification);
    }

    @Override
    protected void execute() {
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
            var errorType = NotificationSenderError.REGULAR;

            try {
                Result result = nodeApi.postNotification(receiverNodeName, createPacket(notification));
                succeeded(result);
                break;
            } catch (Throwable e) {
                errorType = error(e, notification);
                if (errorType == NotificationSenderError.FATAL) {
                    log.info("Notification delivery failed fatally");
                    break;
                }
            }

            Duration totalPeriod = Duration.between(notification.getCreatedAt().toInstant(), Instant.now());
            if (totalPeriod.compareTo(getRetryPeriod(errorType)) < 0) {
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
        packet.setNodeName(nodeName());
        packet.setFullName(fullName());
        packet.setGender(gender());
        if (getAvatar() != null) {
            packet.setAvatar(new AvatarImage(getAvatar()));
        }
        packet.setCreatedAt(Util.toEpochSecond(Util.now()));
        packet.setType(notification.getType().getValue());
        packet.setNotification(objectMapper.writeValueAsString(notification));

        NotificationPacketFingerprint fingerprint = new NotificationPacketFingerprint(packet);
        packet.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
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
        if (result.isOk()) {
            log.info("Notification delivered successfully");
        } else {
            log.info("Receiving node returned error: {}", result.getMessage());
        }
    }

    private Duration getRetryPeriod(NotificationSenderError errorType) {
        return errorType == NotificationSenderError.NAMING ? RETRY_NAME_PERIOD : RETRY_PERIOD;
    }

    private NotificationSenderError error(Throwable e, Notification notification) {
        var errorType = NotificationSenderError.REGULAR;

        if (isUnsubscribeError(e) && notification instanceof SubscriberNotification) {
            SubscriberNotification sn = (SubscriberNotification) notification;
            if (sn.getSubscriptionCreatedAt() != null
                    && sn.getSubscriptionCreatedAt().toInstant().plus(SUBSCRIPTION_DELAY).isAfter(Instant.now())) {
                errorType = NotificationSenderError.REGULAR;
            } else {
                pool.unsubscribe(UUID.fromString(sn.getSubscriberId()));
                errorType = NotificationSenderError.FATAL;
            }
        } else if (e instanceof NodeApiUnknownNameException) {
            errorType = NotificationSenderError.NAMING;
        } else if (e instanceof NodeApiNotFoundException || e instanceof JsonProcessingException) {
            errorType = NotificationSenderError.FATAL;
        }
        failed(e.getMessage());

        return errorType;
    }

    private boolean isUnsubscribeError(Throwable e) {
        if (e instanceof NodeApiValidationException) {
            String errorCode = ((NodeApiValidationException) e).getErrorCode();
            return errorCode.equals("subscription.unsubscribe") || errorCode.equals("notificationPacket.type.unknown");
        }
        return false;
    }

    private void failed(String message) {
        log.error(message);
    }

}
