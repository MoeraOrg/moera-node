package org.moera.node.picker;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import jakarta.inject.Inject;

import org.moera.node.data.Pick;
import org.moera.node.data.PickRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.UniversalContext;
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
public class PickerPool {

    private static final Duration RETRY_MIN_DELAY = Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration RETRY_MAX_DELAY = Duration.of(6, ChronoUnit.HOURS);

    private static final Logger log = LoggerFactory.getLogger(PickerPool.class);

    // We create one picker per remote node to make sure there will not be two threads that download
    // the same posting and step on each other's toes.
    // This also makes it possible in the future to implement fetching several postings in one query.
    private final ConcurrentMap<PickingDirection, Picker> pickers = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Pick> pending = new ConcurrentHashMap<>();

    @Inject
    @Qualifier("pickerTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Transaction tx;

    @Inject
    private PickRepository pickRepository;

    @EventListener(DomainsConfiguredEvent.class)
    public void init() {
        pickRepository.findAll().forEach(p -> pending.put(p.getId(), p));
    }

    public void pick(Pick pick) {
        Instant startedTime = Instant.now();

        pick.setRunning(true);
        if (pick.getId() == null) {
            try {
                pick = storePick(pick);
            } catch (Throwable e) {
                log.error("Error storing a pick", e);
                return;
            }
        }

        Instant storedTime = Instant.now();

        try {
            while (true) {
                Picker picker;
                do {
                    UUID nodeId = pick.getNodeId();
                    picker = pickers.computeIfAbsent(
                        new PickingDirection(nodeId, pick.getRemoteNodeName()),
                        d -> createPicker(d.getNodeName(), nodeId)
                    );
                } while (picker.isStopped());
                try {
                    picker.put(pick);
                } catch (InterruptedException e) {
                    continue;
                }
                break;
            }
        } catch (RejectedExecutionException e) {
            log.warn("Picker was rejected by task executor");
            pickFailed(pick, false);
        } finally {
            Instant finishedTime = Instant.now();
            long fullDuration = startedTime.until(finishedTime, ChronoUnit.MILLIS);
            if (fullDuration > 500) {
                long storeDuration = startedTime.until(storedTime, ChronoUnit.MILLIS);
                long runDuration = storedTime.until(finishedTime, ChronoUnit.MILLIS);
                log.warn("Slow adding picker: {}ms ({}ms..{}ms)", fullDuration, storeDuration, runDuration);
            }
        }
    }

    @Scheduled(fixedDelayString = "PT10S")
    public void retry() {
        pending.values().stream()
            .filter(p -> !p.isRunning())
            .filter(p -> p.getRetryAt() == null || p.getRetryAt().before(Util.now()))
            .forEach(this::pick);
    }

    private Picker createPicker(String nodeName, UUID nodeId) {
        Picker sender = new Picker(this, nodeName);
        taskAutowire.autowireWithoutRequest(sender, nodeId);
        taskExecutor.execute(sender);
        return sender;
    }

    void deletePicker(UUID nodeId, String nodeName) {
        pickers.remove(new PickingDirection(nodeId, nodeName));
    }

    private Pick storePick(final Pick detachedPick) {
        detachedPick.setId(UUID.randomUUID());
        if (detachedPick.getNodeId() == null) {
            detachedPick.setNodeId(universalContext.nodeId());
        }
        Pick pick = tx.executeWrite(() -> pickRepository.saveAndFlush(detachedPick));
        pending.put(pick.getId(), pick);
        return pick;
    }

    private void deletePick(Pick pick) {
        pending.remove(pick.getId());
        tx.executeWriteQuietly(
            () -> pickRepository.findById(pick.getId()).ifPresent(pickRepository::delete),
            e -> log.error("Error deleting pick", e)
        );
    }

    void pickSucceeded(Pick pick) {
        deletePick(pick);
    }

    void pickFailed(Pick pick, boolean fatal) {
        if (fatal) {
            log.info("Pick {} failed fatally", pick.getId());
            deletePick(pick);
            return;
        }

        Duration delay;
        if (pick.getRetryAt() == null) {
            pick.setRetryAt(Util.now());
            delay = RETRY_MIN_DELAY;
        } else {
            delay = Duration.between(pick.getCreatedAt().toInstant(), pick.getRetryAt().toInstant());
        }

        if (delay.compareTo(RETRY_MAX_DELAY) > 0) {
            log.info("Pick {} failed, giving up", pick.getId());
            deletePick(pick);
            return;
        }

        log.info("Pick {} failed, retrying in {}s", pick.getId(), delay.getSeconds());
        pick.setRetryAt(Timestamp.from(pick.getRetryAt().toInstant().plus(delay)));
        tx.executeWriteQuietly(
            () -> {
                pickRepository.saveAndFlush(pick);
            },
            e -> log.error("Error updating pick", e)
        );
        pick.setRunning(false);
    }

}
