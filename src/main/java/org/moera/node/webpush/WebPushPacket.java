package org.moera.node.webpush;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebPushPacket {

    @JsonIgnore
    private UUID nodeId;

    private WebPushPacketType type;
    private String originUrl;
    private String id;
    private StoryInfo story;

    public WebPushPacket() {
    }

    public WebPushPacket(WebPushPacketType type) {
        this.type = type;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public WebPushPacketType getType() {
        return type;
    }

    public void setType(WebPushPacketType type) {
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

    public static WebPushPacket storyAdded(Story story) {
        WebPushPacket packet = new WebPushPacket(WebPushPacketType.STORY_ADDED);
        packet.setNodeId(story.getNodeId());
        packet.setStory(StoryInfo.build(story, true,
                t -> new PostingInfo((Posting) t.getEntry(), true)));
        return packet;
    }

    public static WebPushPacket storyDeleted(UUID nodeId, UUID id) {
        WebPushPacket packet = new WebPushPacket(WebPushPacketType.STORY_DELETED);
        packet.setNodeId(nodeId);
        packet.setId(id.toString());
        return packet;
    }

}
