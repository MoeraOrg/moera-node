package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "pending_jobs")
public class PendingJob {

    @Id
    private UUID id;

    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String jobType;

    @NotNull
    private String parameters;

    private String state;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private int retries;

    private Timestamp waitUntil;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public Timestamp getWaitUntil() {
        return waitUntil;
    }

    public void setWaitUntil(Timestamp waitUntil) {
        this.waitUntil = waitUntil;
    }

}
