package org.moera.node.task;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.PendingJob;
import org.moera.node.data.PendingJobRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.RequestCounter;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Jobs {

    private static final Logger log = LoggerFactory.getLogger(Jobs.class);

    private final Map<UUID, Job<?, ?>> all = new ConcurrentHashMap<>();
    private final BlockingQueue<Job<?, ?>> pending =
            new PriorityBlockingQueue<>(8, Comparator.comparing(Job::getWaitUntil));

    private boolean initialized = false;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private PendingJobRepository pendingJobRepository;

    @Inject
    private Transaction tx;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @EventListener(DomainsConfiguredEvent.class)
    public void init() {
        initialized = true;
        load();
        applicationEventPublisher.publishEvent(new JobsManagerInitializedEvent(this));
    }

    public boolean isReady() {
        return initialized;
    }

    public <P, T extends Job<P, ?>> void run(Class<T> klass, P parameters) {
        run(klass, parameters, null, true);
    }

    public <P, T extends Job<P, ?>> void run(Class<T> klass, P parameters, UUID nodeId) {
        run(klass, parameters, nodeId, true);
    }

    public <P, T extends Job<P, ?>> void runNoPersist(Class<T> klass, P parameters) {
        run(klass, parameters, null, false);
    }

    public <P, T extends Job<P, ?>> void runNoPersist(Class<T> klass, P parameters, UUID nodeId) {
        run(klass, parameters, nodeId, false);
    }

    private <P, T extends Job<P, ?>> void run(Class<T> klass, P parameters, UUID nodeId, boolean persistent) {
        if (!initialized) {
            throw new JobsManagerNotInitializedException();
        }

        T job = null;
        try {
            job = klass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot create a job", e);
        } catch (NoSuchMethodException e) {
            log.error("Cannot find a job constructor", e);
        }
        if (job == null) {
            return;
        }

        job.setParameters(parameters);
        job.setJobs(this);

        if (persistent) {
            persist(job, nodeId);
            if (job.getId() != null) {
                all.put(job.getId(), job);
            }
        }

        if (nodeId != null) {
            taskAutowire.autowireWithoutRequest(job, nodeId);
        } else {
            taskAutowire.autowireWithoutRequestAndDomain(job);
        }
        try {
            taskExecutor.execute(job);
        } catch (RejectedExecutionException e) {
            // No space in the executor, wait a bit
            pending.add(job);
        }
    }

    public boolean isRunning(Class<?> klass) {
        return tx.executeRead(() -> pendingJobRepository.countByType(klass.getCanonicalName())) > 0;
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void load() {
        if (!initialized) {
            return;
        }

        try (var ignored = requestCounter.allot()) {
            log.info("Loading pending jobs");

            Timestamp timestamp = Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS));
            pendingJobRepository.findAllBefore(timestamp).forEach(this::load);
        }
    }

    private void load(PendingJob pendingJob) {
        if (all.containsKey(pendingJob.getId())) {
            return;
        }

        Job<?, ?> job = null;
        try {
            job = (Job<?, ?>) Class.forName(pendingJob.getJobType()).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot create a job", e);
        } catch (NoSuchMethodException e) {
            log.error("Cannot find a job constructor", e);
        } catch (ClassNotFoundException e) {
            log.error("Cannot find a job class", e);
        }
        if (job == null) {
            return;
        }

        try {
            job.setParameters(pendingJob.getParameters(), objectMapper);
            if (pendingJob.getState() != null) {
                job.setState(pendingJob.getState(), objectMapper);
            }
        } catch (JsonProcessingException e) {
            log.error("Cannot load a job", e);
        }

        job.setId(pendingJob.getId());
        job.setRetries(pendingJob.getRetries());
        job.setWaitUntil(pendingJob.getWaitUntil() != null ? pendingJob.getWaitUntil().toInstant() : null);
        job.setJobs(this);

        if (pendingJob.getNodeId() != null) {
            taskAutowire.autowireWithoutRequest(job, pendingJob.getNodeId());
        } else {
            taskAutowire.autowireWithoutRequestAndDomain(job);
        }

        all.put(job.getId(), job);
        if (job.getWaitUntil() != null && job.getWaitUntil().isAfter(Instant.now())) {
            pending.add(job);
        } else {
            try {
                taskExecutor.execute(job);
            } catch (Exception e) {
                // No space in the executor, wait a bit
                pending.add(job);
            }
        }
    }

    private void persist(Job<?, ?> job, UUID nodeId) {
        tx.executeWriteQuietly(
            () -> {
                PendingJob pendingJob = new PendingJob();
                pendingJob.setId(UUID.randomUUID());
                pendingJob.setNodeId(nodeId);
                pendingJob.setJobType(job.getClass().getCanonicalName());
                pendingJob.setParameters(objectMapper.writeValueAsString(job.getParameters()));
                pendingJob.setState(job.getState() != null ? objectMapper.writeValueAsString(job.getState()) : null);
                pendingJob = pendingJobRepository.save(pendingJob);
                job.setId(pendingJob.getId());
            },
            e -> log.error("Error storing job", e)
        );
    }

    private void update(Job<?, ?> job) {
        if (job.getId() == null) {
            return;
        }
        tx.executeWriteQuietly(
            () -> {
                PendingJob pendingJob = pendingJobRepository.findById(job.getId()).orElse(null);
                if (pendingJob == null) {
                    return;
                }
                pendingJob.setState(job.getState() != null ? objectMapper.writeValueAsString(job.getState()) : null);
                pendingJob.setRetries(job.getRetries());
                pendingJob.setWaitUntil(job.getWaitUntil() != null ? Timestamp.from(job.getWaitUntil()) : null);
                pendingJobRepository.save(pendingJob);
            },
            e -> log.error("Error saving job {}", LogUtil.format(job.getId()), e)
        );
    }

    void done(Job<?, ?> job) {
        if (job.getId() == null) {
            return;
        }
        all.remove(job.getId());
        tx.executeWriteQuietly(
            () -> pendingJobRepository.deleteById(job.getId()),
            e -> log.error("Error deleting job {}", LogUtil.format(job.getId()), e)
        );
    }

    void checkpoint(Job<?, ?> job) {
        update(job);
    }

    void retrying(Job<?, ?> job) {
        if (job.getId() == null || job.getWaitUntil().isBefore(Instant.now().plus(1, ChronoUnit.HOURS))) {
            pending.add(job);
        } else {
            // otherwise, it will be destroyed and reconstructed from the database when retry time arrives
            all.remove(job.getId());
        }
        update(job);
    }

    @Scheduled(fixedDelayString = "PT10S")
    public void restartPending() {
        var job = pending.peek();
        while (job != null && job.getWaitUntil().isBefore(Instant.now())) {
            pending.remove();
            try {
                taskExecutor.execute(job);
            } catch (RejectedExecutionException e) {
                // No space in the executor, wait a bit
                pending.add(job);
                return;
            }
            job = pending.peek();
        }
    }

}
