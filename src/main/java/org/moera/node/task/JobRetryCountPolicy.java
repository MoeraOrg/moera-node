package org.moera.node.task;

import java.time.Duration;

public class JobRetryCountPolicy implements JobRetryPolicy {

    private final Job<?, ?> job;
    private final int maxCount;
    private final Duration period;

    public JobRetryCountPolicy(Job<?, ?> job, int maxCount, Duration period) {
        this.job = job;
        this.maxCount = maxCount;
        this.period = period;
    }

    public JobRetryCountPolicy(Job<?, ?> job, int maxCount, String period) {
        this(job, maxCount, Duration.parse(period));
    }

    @Override
    public boolean tryAgain() {
        return job.getRetries() < maxCount;
    }

    @Override
    public Duration waitTime() {
        return period;
    }

}
