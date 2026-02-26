package org.moera.node.notification.send;

import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeApiAuthenticationException;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeApiOperationException;
import org.moera.lib.node.exception.MoeraNodeApiValidationException;
import org.moera.lib.node.types.NotificationPacket;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.SubscriberNotification;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.node.MoeraNodeUnknownNameException;
import org.moera.node.data.ConnectivityStatus;
import org.moera.node.data.PendingNotificationRepository;
import org.moera.node.fingerprint.NotificationPacketFingerprintBuilder;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.operations.RemoteConnectivityOperations;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class NotificationSender extends Task {

    private static final Duration RETRY_MIN_DELAY = Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration RETRY_MAX_DELAY = Duration.of(6, ChronoUnit.HOURS);
    private static final Duration RETRY_PERIOD = Duration.of(7, ChronoUnit.DAYS);
    private static final Duration RETRY_NAME_PERIOD = Duration.of(3, ChronoUnit.HOURS);
    private static final Duration SUBSCRIPTION_DELAY = Duration.of(7, ChronoUnit.MINUTES);
    private static final Duration FAILING_MIN_DELAY = Duration.of(2, ChronoUnit.HOURS);

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private final String receiverNodeName;
    private final BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private Notification notification;
    private ConnectivityStatus connectivityStatus;
    private Duration delay;
    private boolean stopped = false;
    private Instant pausedTill;
    private final NotificationSenderPool pool;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private PendingNotificationRepository pendingNotificationRepository;

    @Inject
    private RemoteConnectivityOperations remoteConnectivityOperations;

    public NotificationSender(NotificationSenderPool pool, String receiverNodeName) {
        this.pool = pool;
        this.receiverNodeName = receiverNodeName;
    }

    public String getReceiverNodeName() {
        return receiverNodeName;
    }

    private ConnectivityStatus getConnectivityStatus() {
        if (connectivityStatus == null) {
            connectivityStatus = remoteConnectivityOperations.getStatus(receiverNodeName);
        }
        return connectivityStatus;
    }

    private void setConnectivityStatus(ConnectivityStatus connectivityStatus) {
        this.connectivityStatus = connectivityStatus;
        remoteConnectivityOperations.setStatus(receiverNodeName, connectivityStatus);
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

    public boolean isPaused() {
        return pausedTill != null;
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
        try {
            while (!stopped) {
                if (notification == null) {
                    try {
                        notification = queue.poll(10, TimeUnit.SECONDS);
                        delay = getConnectivityStatus() == ConnectivityStatus.FAILING ? FAILING_MIN_DELAY : null;
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
        } catch (Throwable e) {
            stopped = true; // if stopped abnormally by exception
        } finally {
            if (stopped) {
                pool.deleteSender(nodeId, receiverNodeName);
                log.debug("Sender from node ID = {} to '{}' stopped", nodeId, receiverNodeName);
            }
        }
    }

    private void deliver(Notification notification) {
        do {
            if (delay != null && pausedTill == null) {
                if (delay.compareTo(Duration.ofSeconds(30)) <= 0) {
                    log.debug("Notification sender paused for {} seconds", delay.toSeconds());
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException e) {
                        // ignore
                    }
                } else {
                    log.debug("Notification sender paused for {} minutes", delay.toMinutes());
                    pausedTill = Instant.now().plus(delay);
                    return;
                }
            }
            pausedTill = null;

            log.info(
                "Delivering notification {} to node '{}' (id = {})",
                notification.getType().name(),
                receiverNodeName,
                LogUtil.format(notification.getPendingNotificationId())
            );
            var errorType = NotificationSenderError.REGULAR;

            try {
                Result result = nodeApi.at(receiverNodeName).sendNotification(createPacket(notification));
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
                retrying();
            } else {
                givingUp();
                break;
            }
        } while (true);
        deletePending(notification);
    }

    private NotificationPacket createPacket(Notification notification) {
        NotificationPacket packet = new NotificationPacket();
        packet.setId(UUID.randomUUID().toString());
        packet.setNodeName(nodeName());
        packet.setFullName(fullName());
        packet.setGender(gender());
        if (getAvatar() != null) {
            packet.setAvatar(AvatarImageUtil.build(getAvatar()));
        }
        packet.setCreatedAt(Util.toEpochSecond(Util.now()));
        packet.setType(notification.getType().getValue());
        packet.setNotification(objectMapper.writeValueAsString(notification));

        byte[] fingerprint = NotificationPacketFingerprintBuilder.build(packet);
        packet.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        packet.setSignatureVersion(NotificationPacketFingerprintBuilder.LATEST_VERSION);

        return packet;
    }

    private void deletePending(Notification notification) {
        UUID pendingNotificationId = Util.uuid(notification.getPendingNotificationId()).orElse(null);
        if (pendingNotificationId == null) {
            return;
        }
        tx.executeWriteQuietly(() -> pendingNotificationRepository.deleteById(pendingNotificationId));
    }

    private void succeeded(Result result) {
        if (result.isOk() || result.isFrozen()) {
            log.info("Notification delivered successfully");
        } else {
            log.info("Receiving node returned error: {}", result.getMessage());
        }
        setConnectivityStatus(result.isFrozen() ? ConnectivityStatus.FROZEN : ConnectivityStatus.NORMAL);
    }

    private NotificationSenderError error(Throwable e, Notification notification) {
        log.debug("Node returned error: {}", LogUtil.format(e.toString()));

        NotificationSenderError errorType;

        if (isUnsubscribeError(e) && notification instanceof SubscriberNotification sn) {
            if (sn.getSubscriptionCreatedAt() != null
                    && sn.getSubscriptionCreatedAt().toInstant().plus(SUBSCRIPTION_DELAY).isAfter(Instant.now())) {
                // Subscription may not be registered by the node yet
                errorType = NotificationSenderError.REGULAR;
            } else {
                pool.unsubscribe(UUID.fromString(sn.getSubscriberId()));
                errorType = NotificationSenderError.FATAL;
            }
        } else if (e instanceof MoeraNodeUnknownNameException) {
            errorType = NotificationSenderError.NAMING;
        } else if (isFatalError(e)) {
            errorType = NotificationSenderError.FATAL;
        } else {
            errorType = NotificationSenderError.REGULAR;
        }
        log.error(e.getMessage());

        return errorType;
    }

    private boolean isUnsubscribeError(Throwable e) {
        if (e instanceof MoeraNodeApiValidationException ve) {
            String errorCode = ve.getErrorCode();
            if (errorCode == null) {
                log.error("Validation error received has null error code");
                return false;
            }
            return errorCode.equals("subscription.unsubscribe") || errorCode.equals("notification.type.unknown");
        }
        return false;
    }

    private boolean isFatalError(Throwable e) {
        if (e instanceof MoeraNodeApiNotFoundException) {
            return true;
        }
        if (e instanceof MoeraNodeApiAuthenticationException) {
            return true;
        }
        if (e instanceof JacksonException) {
            return true;
        }
        if (e instanceof MoeraNodeApiOperationException oe) {
            return oe.getErrorCode().equals("ask.too-many");
        }
        return false;
    }

    private Duration getRetryPeriod(NotificationSenderError errorType) {
        return errorType == NotificationSenderError.NAMING ? RETRY_NAME_PERIOD : RETRY_PERIOD;
    }

    private void retrying() {
        delay = delay == null ? RETRY_MIN_DELAY : delay.multipliedBy(2);
        delay = delay.compareTo(RETRY_MAX_DELAY) < 0 ? delay : RETRY_MAX_DELAY;
        log.info("Notification delivery failed, retry in {}s", delay.toSeconds());
    }

    private void givingUp() {
        log.info("Notification delivery failed, giving up");
        setConnectivityStatus(ConnectivityStatus.FAILING);
    }

}
