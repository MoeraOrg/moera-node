package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.lib.node.types.StoryType;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Story;
import org.moera.node.liberin.Liberin;

public class StoryDeletedLiberin extends Liberin {

    private UUID id;
    private StoryType storyType;
    private String feedName;
    private long moment;
    private UUID postingId;
    private PrincipalFilter viewFilter;

    public StoryDeletedLiberin(Story story) {
        id = story.getId();
        storyType = story.getStoryType();
        feedName = story.getFeedName();
        moment = story.getMoment();
        postingId = story.getEntry() != null ? story.getEntry().getId() : null;
        viewFilter = story.getViewPrincipalFilter();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public PrincipalFilter getViewFilter() {
        return viewFilter;
    }

    public void setViewFilter(PrincipalFilter viewFilter) {
        this.viewFilter = viewFilter;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("id", id);
        model.put("storyType", storyType);
        model.put("feedName", feedName);
        model.put("moment", moment);
        model.put("postingId", postingId);
    }

}
