package org.moera.node.task;

import java.time.Duration;

public class JobExponentialRetryPolicy implements JobRetryPolicy {

    private final Job<?, ?> job;
    private final Duration minPeriod;
    private final Duration maxPeriod;

    public JobExponentialRetryPolicy(Job<?, ?> job, Duration minPeriod, Duration maxPeriod) {
        this.job = job;
        this.minPeriod = minPeriod;
        this.maxPeriod = maxPeriod;
    }

    public JobExponentialRetryPolicy(Job<?, ?> job, String minPeriod, String maxPeriod) {
        this(job, Duration.parse(minPeriod), Duration.parse(maxPeriod));
    }

    @Override
    public boolean tryAgain() {
        return waitTime().compareTo(maxPeriod) <= 0;
    }

    @Override
    public Duration waitTime() {
        return Duration.ofSeconds(Math.round(minPeriod.toSeconds() * Math.pow(2, job.getRetries() - 1)));
    }

}
