package org.moera.node.model.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.Story;
import org.moera.node.data.StoryType;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.StoryInfo;
import org.moera.node.util.Util;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryEvent extends Event {

    private String id;
    private StoryType storyType;
    private String feedName;
    private long publishedAt;
    private boolean pinned;
    private long moment;
    private String postingId;
    private Boolean viewed;
    private Boolean read;
    private AvatarImage summaryAvatar;
    private String summary;
    private String trackingId;
    private String remoteNodeName;
    private String remoteFullName;
    private String remotePostingId;
    private String remoteCommentId;
    private Map<String, String[]> operations;
    @JsonIgnore
    private boolean isAdmin;

    protected StoryEvent(EventType type) {
        super(type);
    }

    protected StoryEvent(EventType type, Story story, boolean isAdmin) {
        super(type);
        StoryInfo storyInfo = StoryInfo.build(story, isAdmin, st -> null);
        id = story.getId().toString();
        storyType = story.getStoryType();
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
        remoteNodeName = story.getRemoteNodeName();
        remoteFullName = story.getRemoteFullName();
        remotePostingId = story.getRemotePostingId();
        remoteCommentId = story.getRemoteCommentId();
        summaryAvatar = storyInfo.getSummaryAvatar();
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

    public AvatarImage getSummaryAvatar() {
        return summaryAvatar;
    }

    public void setSummaryAvatar(AvatarImage summaryAvatar) {
        this.summaryAvatar = summaryAvatar;
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

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getRemoteCommentId() {
        return remoteCommentId;
    }

    public void setRemoteCommentId(String remoteCommentId) {
        this.remoteCommentId = remoteCommentId;
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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("storyType", LogUtil.format(storyType.toString())));
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("pinned", LogUtil.format(pinned)));
        parameters.add(Pair.of("moment", LogUtil.format(moment)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("viewed", LogUtil.format(viewed)));
        parameters.add(Pair.of("read", LogUtil.format(read)));
        parameters.add(Pair.of("summary", LogUtil.format(summary)));
        parameters.add(Pair.of("trackingId", LogUtil.format(trackingId)));
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(remotePostingId)));
        parameters.add(Pair.of("remoteCommentId", LogUtil.format(remoteCommentId)));
    }

}
