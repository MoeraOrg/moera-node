package org.moera.node.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.PendingJob;
import org.moera.node.data.PendingJobRepository;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class Jobs {

    private static final Logger log = LoggerFactory.getLogger(Jobs.class);

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

    public <P, T extends Job<P, ?>> void run(Class<T> klass, P parameters) {
        run(klass, parameters, null);
    }

    public <P, T extends Job<P, ?>> void run(Class<T> klass, P parameters, UUID nodeId) {
        T job = create(getConstructor(klass));
        if (job == null) {
            return;
        }

        job.setParameters(parameters);
        job.setJobs(this);

        persist(job);

        if (nodeId != null) {
            taskAutowire.autowireWithoutRequest(job, nodeId);
        } else {
            taskAutowire.autowireWithoutRequestAndDomain(job);
        }
        taskExecutor.execute(job);
    }

    private <P, T extends Job<P, ?>> Constructor<T> getConstructor(Class<T> klass) {
        if (klass == null) {
            return null;
        }
        try {
            return klass.getConstructor();
        } catch (NoSuchMethodException e) {
            log.error("Cannot find a job constructor", e);
            return null;
        }
    }

    private <P, T extends Job<P, ?>> T create(Constructor<T> constructor) {
        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot create a job", e);
            return null;
        }
    }

    private void persist(Job<?, ?> job) {
        tx.executeWriteQuietly(
            () -> {
                PendingJob pendingJob = new PendingJob();
                pendingJob.setId(UUID.randomUUID());
                job.toPendingJob(pendingJob, objectMapper);
                pendingJob = pendingJobRepository.save(pendingJob);
                job.setId(pendingJob.getId());
            },
            e -> log.error("Error storing job", e)
        );
    }

    public void done(Job<?, ?> job) {
        if (job.getId() != null) {
            tx.executeWriteQuietly(
                () -> pendingJobRepository.deleteById(job.getId()),
                e -> log.error("Error deleting job {}", LogUtil.format(job.getId()), e)
            );
        }
    }

}
