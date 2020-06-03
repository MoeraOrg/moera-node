package org.moera.node.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.model.event.Event;

public class EventPacket {

    @JsonIgnore
    private UUID nodeId;

    private long queueStartedAt;
    private int ordinal;
    private long sentAt;
    private String cid;
    private Event event;

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public long getQueueStartedAt() {
        return queueStartedAt;
    }

    public void setQueueStartedAt(long queueStartedAt) {
        this.queueStartedAt = queueStartedAt;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

}
