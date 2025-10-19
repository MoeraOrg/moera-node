package org.moera.node.model;

public class TooManyRequestsFailure extends RuntimeException {

    private final int limit;
    private final int period;
    private final long retryAfter;

    public TooManyRequestsFailure(int limit, int period, long retryAfter) {
        super("Too many requests");
        this.limit = limit;
        this.period = period;
        this.retryAfter = retryAfter;
    }

    public int getLimit() {
        return limit;
    }

    public int getPeriod() {
        return period;
    }

    public long getRetryAfter() {
        return retryAfter;
    }

}
