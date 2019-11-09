package org.moera.node.event.model;

public class Event {

    private long queueStartedAt;
    private int ordinal;
    private long sentAt;
    private EventType type = EventType.TEST;

    public Event() {
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

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

}
