package org.moera.node.event.model;

import org.moera.node.data.Story;

public class StoryDeletedEvent extends StoryEvent {

    public StoryDeletedEvent() {
        super(EventType.STORY_DELETED);
    }

    public StoryDeletedEvent(Story story) {
        super(EventType.STORY_DELETED, story);
    }

}