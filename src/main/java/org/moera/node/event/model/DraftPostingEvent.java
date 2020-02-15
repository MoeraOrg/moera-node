package org.moera.node.event.model;

import org.moera.node.data.Posting;
import org.moera.node.event.EventSubscriber;

public class DraftPostingEvent extends Event {

    private String id;

    public DraftPostingEvent(EventType type) {
        super(type);
    }

    public DraftPostingEvent(EventType type, Posting posting) {
        super(type);
        this.id = posting.getId().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
