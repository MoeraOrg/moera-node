package org.moera.node.subscriptions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class SubscriptionManager {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    private boolean initialized = false;
    private final Map<UUID, SubscriptionTask> running = new HashMap<>();
    private final Map<UUID, PendingSubscription> pending = new HashMap<>();
    private final Object lock = new Object();
    private final AtomicBoolean rescan = new AtomicBoolean(false);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private PlatformTransactionManager txManager;

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

        synchronized (lock) {
            subscriptionRepository.findPending().forEach(this::add);
        }
    }

    private void add(Subscription subscription) {
        SubscriptionTask task = null;
        if (running.containsKey(subscription.getId()) || pending.containsKey(subscription.getId())) {
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
        try {
            inTransaction(() -> {
                subscriptionRepository.updateRemoteSubscriberIdById(subscriptionId, remoteSubscriberId);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error updating subscription remote subscriber", e);
        }
    }

    void succeededUnsubscribe(UUID subscriptionId) {
        synchronized (lock) {
            running.remove(subscriptionId);
        }
        try {
            inTransaction(() -> {
                subscriptionRepository.deleteById(subscriptionId);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error deleting subscription", e);
        }
    }

    void failed(UUID subscriptionId, Instant createdAt) {
        Instant retryAt = Instant.now();
        synchronized (lock) {
            SubscriptionTask task = running.remove(subscriptionId);
            Duration delay = Duration.between(createdAt, retryAt);
            retryAt = retryAt.plus(delay);
            pending.put(subscriptionId, new PendingSubscription(task.getSubscriptionId(), task.getNodeId(), retryAt));
        }
        Timestamp retryAtTs = Timestamp.from(retryAt);
        try {
            inTransaction(() -> {
                subscriptionRepository.updateRetryAtById(subscriptionId, retryAtTs);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error updating subscription retry timestamp", e);
        }
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
                    .collect(Collectors.toList());
            for (PendingSubscription ps : pendings) {
                pending.remove(ps.getId());
                SubscriptionTask task = new SubscriptionTask(ps.getId());
                running.put(ps.getId(), task);
                taskAutowire.autowireWithoutRequest(task, ps.getNodeId());
                taskExecutor.execute(task);
            }
        }
    }

    private <T> T inTransaction(Callable<T> inside) throws Throwable {
        return Transaction.execute(txManager, inside);
    }

}
