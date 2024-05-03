package org.moera.node.task;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.PendingJob;

public abstract class Job<P, S> extends Task {

    protected P parameters;
    protected S state;

    private Jobs jobs;

    private UUID id;

    void setParameters(P parameters) {
        this.parameters = parameters;
    }

    void setJobs(Jobs jobs) {
        this.jobs = jobs;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void toPendingJob(PendingJob pendingJob, ObjectMapper objectMapper) throws JsonProcessingException {
        pendingJob.setNodeId(nodeId);
        pendingJob.setJobType(getClass().getCanonicalName());
        pendingJob.setParameters(objectMapper.writeValueAsString(parameters));
        pendingJob.setState(state != null ? objectMapper.writeValueAsString(state) : null);
    }

    protected void success() {
        throw new StopJobException(StopJob.SUCCESS);
    }

    private void done() {
        jobs.done(this);
    }

    @Override
    protected void unhandledException(Throwable e) {
        if (!(e instanceof StopJobException)) {
            super.unhandledException(e);
        }
    }

    @Override
    protected void afterExecute() {
        super.afterExecute();
        done();
    }

}
