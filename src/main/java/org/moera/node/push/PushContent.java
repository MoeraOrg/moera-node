package org.moera.node.push;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.FeedWithStatus;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PushContent {

    @JsonIgnore
    private UUID nodeId;

    private PushContentType type;
    private String originUrl;
    private String id;
    private StoryInfo story;
    private FeedWithStatus feedStatus;

    public PushContent() {
    }

    public PushContent(PushContentType type) {
        this.type = type;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public PushContentType getType() {
        return type;
    }

    public void setType(PushContentType type) {
        this.type = type;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
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
        packet.setNodeId(story.getNodeId());
        packet.setStory(StoryInfo.build(story, true,
                t -> new PostingInfo((Posting) t.getEntry(), true)));
        return packet;
    }

    public static PushContent storyDeleted(UUID nodeId, UUID id) {
        PushContent packet = new PushContent(PushContentType.STORY_DELETED);
        packet.setNodeId(nodeId);
        packet.setId(id.toString());
        return packet;
    }

    public static PushContent feedUpdated(UUID nodeId, String feedName, FeedStatus feedStatus) {
        PushContent packet = new PushContent(PushContentType.FEED_UPDATED);
        packet.setNodeId(nodeId);
        packet.setFeedStatus(new FeedWithStatus(feedName, feedStatus));
        return packet;
    }

}
