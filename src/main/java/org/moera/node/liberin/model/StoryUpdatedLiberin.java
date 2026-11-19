package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.liberin.Liberin;

public class StoryUpdatedLiberin extends Liberin {

    private UUID storyId;
    private PrincipalFilter previousViewFilter;
    private Long previousMoment;

    public StoryUpdatedLiberin(UUID storyId) {
        this(storyId, null);
    }

    public StoryUpdatedLiberin(UUID storyId, PrincipalFilter previousViewFilter) {
        this(storyId, previousViewFilter, null);
    }

    public StoryUpdatedLiberin(UUID storyId, PrincipalFilter previousViewFilter, Long previousMoment) {
        this.storyId = storyId;
        this.previousViewFilter = previousViewFilter;
        this.previousMoment = previousMoment;
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

    public Long getPreviousMoment() {
        return previousMoment;
    }

    public void setPreviousMoment(Long previousMoment) {
        this.previousMoment = previousMoment;
    }

}
