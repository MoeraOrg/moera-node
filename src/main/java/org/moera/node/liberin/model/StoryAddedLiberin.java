package org.moera.node.liberin.model;

import java.util.Map;
import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Story;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfoUtil;
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
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        model.put(
            "story",
            StoryInfo.build(
                story,
                true,
                t -> PostingInfoUtil.build(entityManager.merge(t.getEntry()), AccessCheckers.ADMIN)
            )
        );
    }

}
