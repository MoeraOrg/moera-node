package org.moera.node.model.event;

import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Story;

public class StoryAddedEvent extends StoryEvent {

    public StoryAddedEvent() {
        super(EventType.STORY_ADDED);
    }

    public StoryAddedEvent(Story story, boolean isAdmin, DirectServeConfig config) {
        super(EventType.STORY_ADDED, story, isAdmin, config);
    }

}
