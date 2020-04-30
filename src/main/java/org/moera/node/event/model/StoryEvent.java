package org.moera.node.event.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Story;
import org.moera.node.event.EventSubscriber;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryEvent extends Event {

    private String id;
    private String storyType;
    private String feedName;
    private long publishedAt;
    private boolean pinned;
    private long moment;
    private String postingId;
    private Boolean viewed;
    private Boolean read;
    private String summary;
    private String trackingId;
    private String remoteNodeName;
    private String remotePostingId;
    private Map<String, String[]> operations;
    @JsonIgnore
    private boolean isAdmin;

    public StoryEvent(EventType type) {
        super(type);
    }

    public StoryEvent(EventType type, Story story, boolean isAdmin) {
        super(type);
        id = story.getId().toString();
        storyType = story.getStoryType().getValue();
        feedName = story.getFeedName();
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        pinned = story.isPinned();
        moment = story.getMoment();
        postingId = story.getEntry() != null ? story.getEntry().getId().toString() : null;
        if (isAdmin) {
            viewed = story.isViewed();
            read = story.isRead();
            trackingId = story.getTrackingId().toString();
        }
        summary = story.getSummary();
        operations = new HashMap<>();
        operations.put("edit", new String[]{"admin"});
        operations.put("delete", new String[]{"admin"});
        this.isAdmin = isAdmin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoryType() {
        return storyType;
    }

    public void setStoryType(String storyType) {
        this.storyType = storyType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
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

    public Boolean getViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin() == isAdmin;
    }

}
