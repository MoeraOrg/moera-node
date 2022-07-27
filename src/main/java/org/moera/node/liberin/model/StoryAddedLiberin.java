package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;

public class StoryAddedLiberin extends Liberin {

    private Story story;

    public StoryAddedLiberin(Story story) {
        this.story = story;
    }

    public Story getStory() {
        return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("story", StoryInfo.build(story, true,
                t -> new PostingInfo((Posting) t.getEntry(), AccessCheckers.ADMIN)));
    }

}
