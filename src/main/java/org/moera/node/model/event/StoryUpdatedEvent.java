package org.moera.node.model.event;

import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.Story;

public class StoryUpdatedEvent extends StoryEvent {

    public StoryUpdatedEvent() {
        super(EventType.STORY_UPDATED);
    }

    public StoryUpdatedEvent(Story story, boolean isAdmin, DirectServeOperations directServe) {
        super(EventType.STORY_UPDATED, story, isAdmin, directServe);
    }

}
