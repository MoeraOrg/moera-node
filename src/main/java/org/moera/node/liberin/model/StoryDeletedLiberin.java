package org.moera.node.liberin.model;

import org.moera.node.data.Story;
import org.moera.node.liberin.Liberin;

public class StoryDeletedLiberin extends Liberin {

    private Story story;

    public StoryDeletedLiberin(Story story) {
        this.story = story;
    }

    public Story getStory() {
        return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }

}