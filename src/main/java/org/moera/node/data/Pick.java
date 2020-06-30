package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "picks")
public class Pick {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 63)
    private String feedName;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName = "";

    @Size(max = 63)
    private String remoteFeedName = "";

    @NotNull
    @Size(max = 40)
    private String remotePostingId;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp retryAt;

    @Transient
    private boolean running;

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

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getRetryAt() {
        return retryAt;
    }

    public void setRetryAt(Timestamp retryAt) {
        this.retryAt = retryAt;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isSame(EntrySource entrySource) {
        return entrySource.getRemoteFeedName().equals(remoteFeedName)
                && entrySource.getRemoteNodeName().equals(remoteNodeName)
                && entrySource.getRemotePostingId().equals(remotePostingId);
    }

    public void toEntrySource(EntrySource entrySource) {
        entrySource.setRemoteFeedName(remoteFeedName);
        entrySource.setRemoteNodeName(remoteNodeName);
        entrySource.setRemotePostingId(remotePostingId);
    }

}
