package org.moera.node.task;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

public class JobFixedThenExponentialRetryPolicyTest {

    @Test
    void usesFixedIntervalsBeforeSwitchingToExponentialIntervals() {
        var job = new TestJob();
        var policy = new JobFixedThenExponentialRetryPolicy(job, 3, Duration.ofSeconds(10), Duration.ofSeconds(80));

        assertRetry(job, policy, 1, true, 10);
        assertRetry(job, policy, 2, true, 10);
        assertRetry(job, policy, 3, true, 10);
        assertRetry(job, policy, 4, true, 20);
        assertRetry(job, policy, 5, true, 40);
        assertRetry(job, policy, 6, true, 80);
        assertRetry(job, policy, 7, false, 160);
    }

    @Test
    void parsesIsoDurations() {
        var job = new TestJob();
        var policy = new JobFixedThenExponentialRetryPolicy(job, 1, "PT5S", "PT20S");

        assertRetry(job, policy, 1, true, 5);
        assertRetry(job, policy, 2, true, 10);
    }

    private static void assertRetry(
        TestJob job,
        JobRetryPolicy policy,
        int retries,
        boolean tryAgain,
        long waitSeconds
    ) {
        job.setRetries(retries);
        Assertions.assertEquals(tryAgain, policy.tryAgain());
        Assertions.assertEquals(Duration.ofSeconds(waitSeconds), policy.waitTime());
    }

    private static class TestJob extends Job<Object, Object> {

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

}
