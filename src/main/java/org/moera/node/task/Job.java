package org.moera.node.task;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.api.node.NodeApiAuthenticationException;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiNotFoundException;
import org.moera.node.api.node.NodeApiOperationException;
import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.api.node.NodeApiValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Job<P, S> extends Task {

    private static final Logger log = LoggerFactory.getLogger(Job.class);

    protected P parameters;
    protected S state;

    private Jobs jobs;

    private UUID id;
    private int retries;
    private Instant waitUntil;
    private JobRetryPolicy retryPolicy;

    public Job() {
        exponentialRetry("PT10S", "PT3H");
    }

    P getParameters() {
        return parameters;
    }

    void setParameters(P parameters) {
        this.parameters = parameters;
    }

    protected abstract void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException;

    S getState() {
        return state;
    }

    protected abstract void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException;

    void setJobs(Jobs jobs) {
        this.jobs = jobs;
    }

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    int getRetries() {
        return retries;
    }

    void setRetries(int retries) {
        this.retries = retries;
    }

    Instant getWaitUntil() {
        return waitUntil;
    }

    void setWaitUntil(Instant waitUntil) {
        this.waitUntil = waitUntil;
    }

    protected JobRetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    protected void setRetryPolicy(JobRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    protected void noRetry() {
        setRetryPolicy(new JobNoRetryPolicy());
    }

    protected void retryCount(int maxCount, Duration period) {
        setRetryPolicy(new JobRetryCountPolicy(this, maxCount, period));
    }

    protected void retryCount(int maxCount, String period) {
        setRetryPolicy(new JobRetryCountPolicy(this, maxCount, period));
    }

    protected void exponentialRetry(Duration minPeriod, Duration maxPeriod) {
        setRetryPolicy(new JobExponentialRetryPolicy(this, minPeriod, maxPeriod));
    }

    protected void exponentialRetry(String minPeriod, String maxPeriod) {
        setRetryPolicy(new JobExponentialRetryPolicy(this, minPeriod, maxPeriod));
    }

    public void success() {
        throw new StopJobException(StopJob.SUCCESS);
    }

    public void fail() {
        throw new StopJobException(StopJob.FAILURE);
    }

    public void retry() {
        throw new StopJobException(StopJob.RETRY);
    }

    public void checkpoint() {
        retries = 0;
        waitUntil = null;
        jobs.checkpoint(this);
    }

    private void done() {
        jobs.done(this);
    }

    private void recover(Throwable e) {
        if (e != null) {
            log.error("Error executing job {}: {}", this.getClass().getSimpleName(), e.getMessage());
        }

        retries++;
        if (!retryPolicy.tryAgain()) {
            log.info("Giving up");
            failed();
            return;
        }
        Duration duration = retryPolicy.waitTime();
        log.info("Retrying in {}s", duration.toSeconds());
        waitUntil = Instant.now().plus(duration);
        jobs.retrying(this);
    }

    @Override
    protected void handleException(Throwable e) {
        if (e instanceof StopJobException ex) {
            switch (ex.type) {
                case SUCCESS -> succeeded();
                case FAILURE -> failed();
                case RETRY -> recover(null);
            }
        } else if (e instanceof NodeApiException ex && isRecoverableError(ex)) {
            recover(e);
        } else {
            super.handleException(e);
        }
    }

    protected boolean isRecoverableError(NodeApiException e) {
        if (e instanceof NodeApiUnknownNameException) {
            return false;
        }
        if (e instanceof NodeApiValidationException) {
            return false;
        }
        if (e instanceof NodeApiAuthenticationException) {
            return false;
        }
        if (e instanceof NodeApiNotFoundException) {
            return false;
        }
        if (e instanceof NodeApiOperationException ex) {
            String errorCode = ex.getErrorCode();
            return !errorCode.equals("ask.too-many");
        }
        return true;
    }

    @Override
    protected void started() {
        if (retries == 0) {
            log.info("Executing job {}", this.getClass().getSimpleName());
        } else {
            log.info("Executing job {}, retry {}", this.getClass().getSimpleName(), retries);
        }
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        done();
    }

    @Override
    protected void failed() {
        super.failed();
        done();
    }

}
