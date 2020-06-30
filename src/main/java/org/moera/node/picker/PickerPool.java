package org.moera.node.picker;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

import org.moera.node.data.Pick;
import org.moera.node.data.PickRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class PickerPool {

    private static final long RETRY_MIN_DELAY = 30;
    private static final long RETRY_MAX_DELAY = 3 * 60 * 60;

    private static Logger log = LoggerFactory.getLogger(PickerPool.class);

    // We create one picker per remote node to make sure there will not be two threads that download
    // the same posting and step on each other's toes.
    // This also makes possible in the future to implement fetching several postings in one query.
    private ConcurrentMap<PickingDirection, Picker> pickers = new ConcurrentHashMap<>();
    private ConcurrentMap<UUID, Pick> pending = new ConcurrentHashMap<>();

    @Inject
    @Qualifier("pickerTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RequestContext requestContext;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private PickRepository pickRepository;

    public void pick(Pick pick) {
        pick.setRunning(true);
        if (pick.getId() == null) {
            try {
                pick = storePick(pick);
            } catch (Throwable e) {
                log.error("Error storing a pick", e);
                return;
            }
        }

        while (true) {
            Picker picker;
            do {
                UUID nodeId = pick.getNodeId();
                picker = pickers.computeIfAbsent(
                        new PickingDirection(nodeId, pick.getRemoteNodeName()),
                        d -> createPicker(d.getNodeName(), nodeId));
            } while (picker.isStopped());
            try {
                picker.put(pick);
            } catch (InterruptedException e) {
                continue;
            }
            break;
        }
    }

    @Scheduled(fixedDelayString = "PT10S")
    public void retry() {
        pending.values().stream()
                .filter(p -> !p.isRunning())
                .filter(p -> p.getRetryAt() != null)
                .filter(p -> p.getRetryAt().before(Util.now()))
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

    private Pick storePick(final Pick detachedPick) throws Throwable {
        detachedPick.setId(UUID.randomUUID());
        detachedPick.setNodeId(requestContext.nodeId());
        Pick pick = inTransaction(() -> pickRepository.save(detachedPick));
        pending.put(pick.getId(), pick);
        return pick;
    }

    private void deletePick(Pick pick) {
        pending.remove(pick.getId());
        try {
            inTransaction(() -> {
                pickRepository.deleteById(pick.getId());
                return null;
            });
        } catch (Throwable e) {
            log.error("Error deleting pick", e);
        }
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

        long delay;
        if (pick.getRetryAt() == null) {
            pick.setRetryAt(Util.now());
            delay = RETRY_MIN_DELAY;
        } else {
            delay = Util.toEpochSecond(pick.getRetryAt()) - Util.toEpochSecond(pick.getCreatedAt());
        }

        if (delay > RETRY_MAX_DELAY) {
            log.info("Pick {} failed, all retries failed", pick.getId());
            deletePick(pick);
            return;
        }

        log.info("Pick {} failed, retrying in {}s", pick.getId(), delay);
        pick.setRetryAt(Timestamp.from(pick.getRetryAt().toInstant().plusSeconds(delay)));
        try {
            inTransaction(() -> {
                pickRepository.save(pick);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error updating pick", e);
        }
        pick.setRunning(false);
    }

    private <T> T inTransaction(Callable<T> inside) throws Throwable {
        return Transaction.execute(txManager, inside);
    }

}
