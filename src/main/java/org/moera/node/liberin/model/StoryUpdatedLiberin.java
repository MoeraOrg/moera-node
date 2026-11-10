package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.liberin.Liberin;

public class StoryUpdatedLiberin extends Liberin {

    private UUID storyId;
    private PrincipalFilter previousViewFilter;

    public StoryUpdatedLiberin(UUID storyId) {
        this(storyId, null);
    }

    public StoryUpdatedLiberin(UUID storyId, PrincipalFilter previousViewFilter) {
        this.storyId = storyId;
        this.previousViewFilter = previousViewFilter;
    }

    public UUID getStoryId() {
        return storyId;
    }

    public void setStoryId(UUID storyId) {
        this.storyId = storyId;
    }

    public PrincipalFilter getPreviousViewFilter() {
        return previousViewFilter;
    }

    public void setPreviousViewFilter(PrincipalFilter previousViewFilter) {
        this.previousViewFilter = previousViewFilter;
    }

}
