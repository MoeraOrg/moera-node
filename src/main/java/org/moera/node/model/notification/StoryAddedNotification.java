package org.moera.node.model.notification;

import java.util.List;

import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Story;
import org.moera.node.data.StoryType;
import org.springframework.data.util.Pair;

public class StoryAddedNotification extends SubscriberNotification {

    @Size(max = 36)
    private String storyId;

    @Size(max = 63)
    private String feedName;

    @Size(max = 63)
    private StoryType storyType;

    @Size(max = 36)
    private String postingId;

    public StoryAddedNotification() {
        super(NotificationType.STORY_ADDED);
    }

    public StoryAddedNotification(Story story) {
        super(NotificationType.STORY_ADDED);
        this.storyId = story.getId().toString();
        this.feedName = story.getFeedName();
        this.storyType = story.getStoryType();
        if (story.getEntry() != null) {
            this.postingId = story.getEntry().getId().toString();
        }
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("storyId", LogUtil.format(storyId)));
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("storyType", LogUtil.format(storyType.getValue())));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
    }

}
