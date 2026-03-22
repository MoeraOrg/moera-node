package org.moera.node.model.event;

import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Story;

public class StoryUpdatedEvent extends StoryEvent {

    public StoryUpdatedEvent() {
        super(EventType.STORY_UPDATED);
    }

    public StoryUpdatedEvent(Story story, boolean isAdmin, DirectServeConfig config) {
        super(EventType.STORY_UPDATED, story, isAdmin, config);
    }

}
