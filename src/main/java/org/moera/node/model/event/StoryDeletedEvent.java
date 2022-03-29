package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.Story;
import org.moera.node.data.StoryType;
import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryDeletedEvent extends Event {

    private String id;
    private StoryType storyType;
    private String feedName;
    private long moment;
    private String postingId;
    @JsonIgnore
    private boolean isAdmin;

    public StoryDeletedEvent() {
        super(EventType.STORY_DELETED);
    }

    public StoryDeletedEvent(Story story, boolean isAdmin) {
        super(EventType.STORY_DELETED);
        id = story.getId().toString();
        storyType = story.getStoryType();
        feedName = story.getFeedName();
        moment = story.getMoment();
        postingId = story.getEntry() != null ? story.getEntry().getId().toString() : null;
        this.isAdmin = isAdmin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin() == isAdmin;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("storyType", LogUtil.format(storyType.toString())));
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("moment", LogUtil.format(moment)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
    }

}
