package org.moera.node.task;

import java.time.Duration;

public class JobFixedThenExponentialRetryPolicy implements JobRetryPolicy {

    private final Job<?, ?> job;
    private final int fixedCount;
    private final Duration period;
    private final Duration maxPeriod;

    public JobFixedThenExponentialRetryPolicy(
        Job<?, ?> job,
        int fixedCount,
        Duration period,
        Duration maxPeriod
    ) {
        this.job = job;
        this.fixedCount = fixedCount;
        this.period = period;
        this.maxPeriod = maxPeriod;
    }

    public JobFixedThenExponentialRetryPolicy(Job<?, ?> job, int fixedCount, String period, String maxPeriod) {
        this(job, fixedCount, Duration.parse(period), Duration.parse(maxPeriod));
    }

    @Override
    public boolean tryAgain() {
        return waitTime().compareTo(maxPeriod) <= 0;
    }

    @Override
    public Duration waitTime() {
        int exponent = Math.max(0, job.getRetries() - fixedCount);
        return Duration.ofSeconds(Math.round(period.toSeconds() * Math.pow(2, exponent)));
    }

}
