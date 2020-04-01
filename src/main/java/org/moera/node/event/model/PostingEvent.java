package org.moera.node.event.model;

import org.moera.node.data.Posting;

public class PostingEvent extends Event {

    private String id;

    public PostingEvent(EventType type) {
        super(type);
    }

    public PostingEvent(EventType type, Posting posting) {
        super(type);
        this.id = posting.getId().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
