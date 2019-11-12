package org.moera.node.event.model;

import java.util.UUID;

import org.moera.node.data.Posting;

public class PostingEvent extends Event {

    private UUID id;
    private long moment;

    public PostingEvent(EventType type) {
        super(type);
    }

    public PostingEvent(EventType type, Posting posting) {
        super(type);
        this.id = posting.getId();
        this.moment = posting.getCurrentRevision().getMoment();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

}
