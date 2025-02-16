package org.moera.node.push;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.FeedWithStatus;
import org.moera.lib.node.types.PushContentType;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Story;
import org.moera.node.model.FeedWithStatusUtil;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.operations.MediaAttachmentsProvider;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PushContent {

    private PushContentType type;
    private String id;
    private StoryInfo story;
    private FeedWithStatus feedStatus;

    public PushContent() {
    }

    public PushContent(PushContentType type) {
        this.type = type;
    }

    public PushContentType getType() {
        return type;
    }

    public void setType(PushContentType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StoryInfo getStory() {
        return story;
    }

    public void setStory(StoryInfo story) {
        this.story = story;
    }

    public FeedWithStatus getFeedStatus() {
        return feedStatus;
    }

    public void setFeedStatus(FeedWithStatus feedStatus) {
        this.feedStatus = feedStatus;
    }

    public static PushContent storyAdded(Story story) {
        PushContent packet = new PushContent(PushContentType.STORY_ADDED);
        packet.setStory(StoryInfo.build(story, true,
                t -> new PostingInfo(t.getEntry(), MediaAttachmentsProvider.NONE, AccessCheckers.ADMIN)));
        return packet;
    }

    public static PushContent storyDeleted(UUID id) {
        PushContent packet = new PushContent(PushContentType.STORY_DELETED);
        packet.setId(id.toString());
        return packet;
    }

    public static PushContent feedUpdated(String feedName, FeedStatus feedStatus) {
        PushContent packet = new PushContent(PushContentType.FEED_UPDATED);
        packet.setFeedStatus(FeedWithStatusUtil.build(feedName, feedStatus));
        return packet;
    }

}
