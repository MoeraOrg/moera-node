package org.moera.node.task;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.data.PendingJob;
import org.moera.node.data.PendingJobRepository;
import org.moera.node.util.Transaction;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

public class JobsTest {

    public static class Parameters {

        private String value;

        public Parameters() {
        }

        public Parameters(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public static class TestJob extends Job<Parameters, Object> {

        public TestJob() {
        }

        @Override
        protected void setParameters(String parameters, ObjectMapper objectMapper) {
            this.parameters = objectMapper.readValue(parameters, Parameters.class);
        }

        @Override
        protected void setState(String state, ObjectMapper objectMapper) {
            this.state = null;
        }

        @Override
        protected void execute() {
        }

        public UUID jobId() {
            return getId();
        }

    }

    public static class UnconstructableJob extends Job<Parameters, Object> {

        private UnconstructableJob() {
        }

        @Override
        protected void setParameters(String parameters, ObjectMapper objectMapper) {
        }

        @Override
        protected void setState(String state, ObjectMapper objectMapper) {
        }

        @Override
        protected void execute() {
        }

    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void legacyPersistentJobStartsImmediatelyInsideTransaction() {
        AtomicReference<PendingJob> saved = new AtomicReference<>();
        List<Runnable> executed = new ArrayList<>();
        Jobs jobs = jobs(saved, executed, null);
        beginTransaction();

        jobs.run(TestJob.class, new Parameters("value"));

        Assertions.assertNotNull(saved.get().getId());
        Assertions.assertEquals(TestJob.class.getCanonicalName(), saved.get().getJobType());
        Assertions.assertEquals("{\"value\":\"value\"}", saved.get().getParameters());
        Assertions.assertEquals(1, executed.size());
        Assertions.assertEquals(saved.get().getId(), ((TestJob) executed.getFirst()).jobId());
        completeTransaction(false);
    }

    @Test
    void legacyPersistenceFailureIsIgnoredAndJobStarts() {
        List<Runnable> executed = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("persistence failed");
        Jobs jobs = jobs(new AtomicReference<>(), executed, failure);

        jobs.run(TestJob.class, new Parameters("value"));

        Assertions.assertEquals(1, executed.size());
        Assertions.assertNull(((TestJob) executed.getFirst()).jobId());
    }

    @Test
    void legacyJobCreationFailureIsPropagated() {
        Jobs jobs = jobs(new AtomicReference<>(), new ArrayList<>(), null);

        IllegalStateException thrown = Assertions.assertThrows(
            IllegalStateException.class,
            () -> jobs.run(UnconstructableJob.class, new Parameters("value"))
        );

        Assertions.assertEquals("Cannot create job " + UnconstructableJob.class.getCanonicalName(), thrown.getMessage());
    }

    @Test
    void transactionalJobStartsImmediatelyWithoutTransactionAndReturnsId() {
        AtomicReference<PendingJob> saved = new AtomicReference<>();
        List<Runnable> executed = new ArrayList<>();
        Jobs jobs = jobs(saved, executed, null);

        UUID id = jobs.runAfterCommit(TestJob.class, new Parameters("value"));

        Assertions.assertNotNull(id);
        Assertions.assertEquals(id, saved.get().getId());
        Assertions.assertEquals(1, executed.size());
        Assertions.assertEquals(id, ((TestJob) executed.getFirst()).jobId());
    }

    @Test
    void transactionalJobStartsOnlyAfterCommit() {
        AtomicReference<PendingJob> saved = new AtomicReference<>();
        List<Runnable> executed = new ArrayList<>();
        Jobs jobs = jobs(saved, executed, null);
        beginTransaction();

        UUID id = jobs.runAfterCommit(TestJob.class, new Parameters("value"));

        Assertions.assertEquals(id, saved.get().getId());
        Assertions.assertTrue(executed.isEmpty());

        completeTransaction(true);

        Assertions.assertEquals(1, executed.size());
        Assertions.assertEquals(id, ((TestJob) executed.getFirst()).jobId());
    }

    @Test
    void transactionalJobDoesNotStartAfterRollback() {
        AtomicReference<PendingJob> saved = new AtomicReference<>();
        List<Runnable> executed = new ArrayList<>();
        Jobs jobs = jobs(saved, executed, null);
        beginTransaction();

        UUID id = jobs.runAfterCommit(TestJob.class, new Parameters("value"));

        Assertions.assertEquals(id, saved.get().getId());
        completeTransaction(false);

        Assertions.assertTrue(executed.isEmpty());
    }

    @Test
    void transactionalPersistenceFailureIsPropagated() {
        List<Runnable> executed = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("persistence failed");
        Jobs jobs = jobs(new AtomicReference<>(), executed, failure);

        RuntimeException thrown = Assertions.assertThrows(
            RuntimeException.class,
            () -> jobs.runAfterCommit(TestJob.class, new Parameters("value"))
        );

        Assertions.assertSame(failure, thrown);
        Assertions.assertTrue(executed.isEmpty());
    }

    @Test
    void nonPersistentJobStartsImmediatelyInsideTransaction() {
        AtomicReference<PendingJob> saved = new AtomicReference<>();
        List<Runnable> executed = new ArrayList<>();
        Jobs jobs = jobs(saved, executed, null);
        beginTransaction();

        jobs.runNoPersist(TestJob.class, new Parameters("value"));

        Assertions.assertNull(saved.get());
        Assertions.assertEquals(1, executed.size());
        Assertions.assertNull(((TestJob) executed.getFirst()).jobId());
        completeTransaction(false);
    }

    private static Jobs jobs(
        AtomicReference<PendingJob> saved,
        List<Runnable> executed,
        RuntimeException persistenceFailure
    ) {
        PendingJobRepository pendingJobRepository = (PendingJobRepository) Proxy.newProxyInstance(
            PendingJobRepository.class.getClassLoader(),
            new Class<?>[] {PendingJobRepository.class},
            (proxy, method, args) -> {
                if (method.getName().equals("save")) {
                    if (persistenceFailure != null) {
                        throw persistenceFailure;
                    }
                    PendingJob pendingJob = (PendingJob) args[0];
                    saved.set(pendingJob);
                    return pendingJob;
                }
                return null;
            }
        );
        TaskAutowire taskAutowire = new TaskAutowire() {

            @Override
            public void autowireWithoutRequest(Task task, UUID nodeId) {
            }

            @Override
            public void autowireWithoutRequestAndDomain(Task task) {
            }

        };

        Jobs jobs = new Jobs();
        ReflectionTestUtils.setField(jobs, "initialized", true);
        ReflectionTestUtils.setField(jobs, "pendingJobRepository", pendingJobRepository);
        ReflectionTestUtils.setField(jobs, "tx", new Transaction());
        ReflectionTestUtils.setField(jobs, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(jobs, "taskExecutor", (TaskExecutor) executed::add);
        ReflectionTestUtils.setField(jobs, "taskAutowire", taskAutowire);
        return jobs;
    }

    private static void beginTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private static void completeTransaction(boolean committed) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        if (committed) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        int status = committed
            ? TransactionSynchronization.STATUS_COMMITTED
            : TransactionSynchronization.STATUS_ROLLED_BACK;
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
        TransactionSynchronizationManager.clear();
    }

}
