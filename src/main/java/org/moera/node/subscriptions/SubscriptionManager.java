package org.moera.node.subscriptions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.RequestCounter;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionManager {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    private static final Duration MAX_RETRY_DELAY = Duration.ofDays(14);

    private boolean initialized = false;
    private final Map<UUID, SubscriptionTask> running = new HashMap<>();
    private final Map<UUID, PendingSubscription> pending = new HashMap<>();
    private final Object lock = new Object();
    private final AtomicBoolean rescan = new AtomicBoolean(false);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Transaction tx;

    @EventListener(DomainsConfiguredEvent.class)
    public void init() {
        initialized = true;
        scan();
    }

    @Scheduled(fixedDelayString = "PT10M")
    public void scan() {
        if (!initialized) {
            return;
        }

        try (var ignored = requestCounter.allot()) {
            log.info("Rescanning subscriptions to be realized");

            Collection<Subscription> list = subscriptionRepository.findPending();
            list.addAll(subscriptionRepository.findUnused());
            synchronized (lock) {
                list.forEach(this::add);
            }
        }
    }

    private void add(Subscription subscription) {
        SubscriptionTask task = null;
        if (running.containsKey(subscription.getId())
                || pending.containsKey(subscription.getId())
                || subscription.getRetryAt() != null
                    && subscription.getRetryAt().toInstant().isAfter(Instant.now().plus(1, ChronoUnit.HOURS))) {
            return;
        }
        if (subscription.getRetryAt() == null || subscription.getRetryAt().toInstant().isBefore(Instant.now())) {
            task = new SubscriptionTask(subscription.getId());
            running.put(subscription.getId(), task);
        } else {
            pending.put(
                    subscription.getId(),
                    new PendingSubscription(
                            subscription.getId(),
                            subscription.getNodeId(),
                            subscription.getRetryAt().toInstant()
                    )
            );
        }
        if (task != null) {
            taskAutowire.autowireWithoutRequest(task, subscription.getNodeId());
            taskExecutor.execute(task);
        }
    }

    void noAction(UUID subscriptionId) {
        synchronized (lock) {
            running.remove(subscriptionId);
        }
    }

    void succeededSubscribe(UUID subscriptionId, String remoteSubscriberId) {
        synchronized (lock) {
            running.remove(subscriptionId);
        }
        tx.executeWriteQuietly(
            () -> subscriptionRepository.updateRemoteSubscriberIdById(subscriptionId, remoteSubscriberId),
            e -> log.error("Error updating subscription remote subscriber", e)
        );
    }

    void succeededUnsubscribe(UUID subscriptionId) {
        synchronized (lock) {
            running.remove(subscriptionId);
        }
        tx.executeWriteQuietly(
            () -> subscriptionRepository.deleteById(subscriptionId),
            e -> log.error("Error deleting subscription", e)
        );
    }

    void failed(Subscription subscription) {
        Instant retryAt = Instant.now();
        boolean fatal;
        synchronized (lock) {
            SubscriptionTask task = running.remove(subscription.getId());
            Duration delay = Duration.between(subscription.getCreatedAt().toInstant(), retryAt);
            fatal = delay.compareTo(MAX_RETRY_DELAY) >= 0;
            if (!fatal) {
                retryAt = retryAt.plus(delay);
                pending.put(subscription.getId(),
                        new PendingSubscription(task.getSubscriptionId(), task.getNodeId(), retryAt));
            }
        }
        if (!fatal) {
            Timestamp retryAtTs = Timestamp.from(retryAt);
            tx.executeWriteQuietly(
                () -> subscriptionRepository.updateRetryAtById(subscription.getId(), retryAtTs),
                e -> log.error("Error updating subscription retry timestamp", e)
            );
        } else {
            log.info("Subscription {} cannot be established", subscription.getId());
            tx.executeWriteQuietly(
                () -> {
                    subscriptionRepository.updateRetryAtById(subscription.getId(), Util.farFuture());
                    subscriptionOperations.deleteParents(subscription);
                    // The subscription itself becomes a pending unused subscription and will be removed by trigger
                },
                e -> log.error("Error deleting hopeless subscription", e)
            );
        }
    }

    void subscriptionInvalid(Subscription subscription) {
        log.info("Subscription {} is invalid", subscription.getId());

        synchronized (lock) {
            running.remove(subscription.getId());
        }
        tx.executeWriteQuietly(
            () -> {
                subscriptionRepository.updateRetryAtById(subscription.getId(), Util.farFuture());
                subscriptionOperations.deleteParents(subscription);
                // The subscription itself becomes a pending unused subscription and will be removed by trigger
            },
            e -> log.error("Error deleting invalid subscription", e)
        );
    }

    public void rescan() {
        rescan.set(true);
    }

    @Scheduled(fixedDelayString = "PT10S")
    public void refresh() {
        if (!initialized) {
            return;
        }

        retry();
        if (rescan.getAndSet(false)) {
            scan();
        }
    }

    private void retry() {
        synchronized (lock) {
            List<PendingSubscription> pendings = pending.values().stream()
                    .filter(ps -> ps.getRetryAt().isBefore(Instant.now()))
                    .toList();
            for (PendingSubscription ps : pendings) {
                pending.remove(ps.getId());
                SubscriptionTask task = new SubscriptionTask(ps.getId());
                running.put(ps.getId(), task);
                taskAutowire.autowireWithoutRequest(task, ps.getNodeId());
                taskExecutor.execute(task);
            }
        }
    }

}
