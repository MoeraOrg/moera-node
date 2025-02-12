package org.moera.node.model.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Story;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.StoryInfo;
import org.moera.node.model.StorySummaryData;
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
    private Boolean satisfied;
    private String summaryNodeName;
    private String summaryFullName;
    private AvatarImage summaryAvatar;
    private String summary;
    private StorySummaryData summaryData;
    private String remoteNodeName;
    private String remoteFullName;
    private String remotePostingId;
    private String remoteCommentId;
    private Map<String, Principal> operations;

    protected StoryEvent(EventType type) {
        super(type, Scope.VIEW_FEEDS);
    }

    protected StoryEvent(EventType type, Story story, boolean isAdmin) { // See also StoryInfo constructor
        super(type, Scope.VIEW_FEEDS,
                isAdmin ? Principal.ADMIN : story.getViewPrincipalFilter().a().andNot(Principal.ADMIN));
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
            satisfied = story.isSatisfied();
        }
        remoteNodeName = story.getRemoteNodeName();
        remoteFullName = story.getRemoteFullName();
        remotePostingId = story.getRemotePostingId();
        remoteCommentId = story.getRemoteCommentId();
        summaryNodeName = storyInfo.getSummaryNodeName();
        summaryFullName = storyInfo.getSummaryFullName();
        summaryAvatar = storyInfo.getSummaryAvatar();
        if (story.getSummary().startsWith("{")) {
            summaryData = story.getSummaryData();
        } else if (story.getSummary().isEmpty()) {
            summaryData = new StorySummaryData();
        } else {
            summary = story.getSummary();
        }
        operations = new HashMap<>();
        operations.put("edit", Principal.ADMIN);
        operations.put("delete", Principal.ADMIN);
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

    public Boolean getSatisfied() {
        return satisfied;
    }

    public void setSatisfied(Boolean satisfied) {
        this.satisfied = satisfied;
    }

    public String getSummaryNodeName() {
        return summaryNodeName;
    }

    public void setSummaryNodeName(String summaryNodeName) {
        this.summaryNodeName = summaryNodeName;
    }

    public String getSummaryFullName() {
        return summaryFullName;
    }

    public void setSummaryFullName(String summaryFullName) {
        this.summaryFullName = summaryFullName;
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

    public StorySummaryData getSummaryData() {
        return summaryData;
    }

    public void setSummaryData(StorySummaryData summaryData) {
        this.summaryData = summaryData;
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

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
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
        parameters.add(Pair.of("satisfied", LogUtil.format(satisfied)));
        parameters.add(Pair.of("summary", LogUtil.format(summary)));
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(remotePostingId)));
        parameters.add(Pair.of("remoteCommentId", LogUtil.format(remoteCommentId)));
    }

}
