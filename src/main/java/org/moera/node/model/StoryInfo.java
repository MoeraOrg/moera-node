package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Story;
import org.moera.node.data.StoryType;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryInfo {

    private String id;
    private String feedName;
    private StoryType storyType;
    private long createdAt;
    private long publishedAt;
    private boolean pinned;
    private Long moment;
    private Boolean viewed;
    private Boolean read;
    private Boolean satisfied;
    private String summaryNodeName;
    private String summaryFullName;
    private AvatarImage summaryAvatar;
    private String summary;
    private StorySummaryData summaryData;
    private String trackingId;
    private PostingInfo posting;
    private CommentInfo comment;
    private String remoteNodeName;
    private String remoteFullName;
    private String remotePostingId;
    private String remoteCommentId;
    private String remoteMediaId;
    private Map<String, Principal> operations;

    public StoryInfo() {
    }

    private StoryInfo(Story story, boolean isAdmin) {
        id = story.getId().toString();
        feedName = story.getFeedName();
        storyType = story.getStoryType();
        createdAt = Util.toEpochSecond(story.getCreatedAt());
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        pinned = story.isPinned();
        moment = story.getMoment();
        if (isAdmin) {
            viewed = story.isViewed();
            read = story.isRead();
            satisfied = story.isSatisfied();
            trackingId = story.getTrackingId().toString();
        }
        if (story.getSummary().startsWith("{")) {
            summaryData = story.getSummaryData();
        } else {
            summary = story.getSummary();
        }
        operations = new HashMap<>();
        operations.put("edit", Principal.ADMIN);
        operations.put("delete", Principal.ADMIN);
    }

    public static StoryInfo build(Story story, boolean isAdmin,
                                  Function<Story, PostingInfo> buildPostingInfo) {
        StoryInfo info = new StoryInfo(story, isAdmin);
        switch (story.getStoryType()) {
            case POSTING_ADDED:
                info.setPosting(buildPostingInfo.apply(story));
                break;

            case REACTION_ADDED_POSITIVE:
            case REACTION_ADDED_NEGATIVE:
                info.setPosting(new PostingInfo(story.getEntry().getId()));
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteOwnerAvatarMediaFile(),
                                    story.getRemoteOwnerAvatarShape()));
                }
                break;

            case MENTION_POSTING:
            case POSTING_SUBSCRIBE_TASK_FAILED:
            case POSTING_UPDATED:
            case POSTING_REACTION_TASK_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemotePostingAvatarMediaFile(),
                                    story.getRemotePostingAvatarShape()));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                break;

            case POSTING_UPDATE_TASK_FAILED:
            case BLOCKED_USER_IN_POSTING:
            case UNBLOCKED_USER_IN_POSTING:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteNodeName());
                info.setSummaryFullName(story.getRemoteFullName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteAvatarMediaFile(),
                                    story.getRemoteAvatarShape()));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                break;

            case SUBSCRIBER_ADDED:
            case SUBSCRIBER_DELETED:
            case POSTING_POST_TASK_FAILED:
            case FRIEND_ADDED:
            case FRIEND_DELETED:
            case FRIEND_GROUP_DELETED:
            case ASKED_TO_SUBSCRIBE:
            case ASKED_TO_FRIEND:
            case BLOCKED_USER:
            case UNBLOCKED_USER:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteNodeName());
                info.setSummaryFullName(story.getRemoteFullName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteAvatarMediaFile(),
                                    story.getRemoteAvatarShape()));
                }
                break;

            case COMMENT_ADDED:
                info.setPosting(new PostingInfo(story.getEntry().getId()));
                info.setRemoteCommentId(story.getRemoteCommentId());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteOwnerAvatarMediaFile(),
                                    story.getRemoteOwnerAvatarShape()));
                }
                break;

            case MENTION_COMMENT:
            case REPLY_COMMENT:
            case COMMENT_REACTION_ADDED_POSITIVE:
            case COMMENT_REACTION_ADDED_NEGATIVE:
            case COMMENT_REACTION_TASK_FAILED:
            case REMOTE_COMMENT_ADDED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteOwnerAvatarMediaFile(),
                                    story.getRemoteOwnerAvatarShape()));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                info.setRemoteCommentId(story.getRemoteCommentId());
                break;

            case COMMENT_UPDATE_TASK_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemotePostingAvatarMediaFile(),
                                    story.getRemotePostingAvatarShape()));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                info.setRemoteCommentId(story.getRemoteCommentId());
                break;

            case COMMENT_POST_TASK_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemotePostingAvatarMediaFile(),
                                    story.getRemotePostingAvatarShape()));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                break;

            case POSTING_MEDIA_REACTION_ADDED_POSITIVE:
            case POSTING_MEDIA_REACTION_ADDED_NEGATIVE:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteOwnerAvatarMediaFile(),
                                    story.getRemoteOwnerAvatarShape()));
                }
                info.setRemotePostingId(story.getRemoteParentPostingId());
                info.setRemoteMediaId(story.getRemoteParentMediaId());
                break;

            case COMMENT_MEDIA_REACTION_ADDED_POSITIVE:
            case COMMENT_MEDIA_REACTION_ADDED_NEGATIVE:
            case COMMENT_MEDIA_REACTION_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemoteOwnerAvatarMediaFile(),
                                    story.getRemoteOwnerAvatarShape()));
                }
                info.setRemotePostingId(story.getRemoteParentPostingId());
                info.setRemoteCommentId(story.getRemoteParentCommentId());
                info.setRemoteMediaId(story.getRemoteParentMediaId());
                break;

            case POSTING_MEDIA_REACTION_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(
                                    story.getRemotePostingAvatarMediaFile(),
                                    story.getRemotePostingAvatarShape()));
                }
                info.setRemotePostingId(story.getRemoteParentPostingId());
                info.setRemoteMediaId(story.getRemoteParentMediaId());
                break;

            case SHERIFF_MARKED:
            case SHERIFF_UNMARKED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setSummaryNodeName(story.getSummaryData().getSheriff().getSheriffName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(
                            new AvatarImage(story.getRemoteAvatarMediaFile(), story.getRemoteAvatarShape()));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                info.setRemoteCommentId(story.getRemoteCommentId());
                break;
        }
        return info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
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

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public PostingInfo getPosting() {
        return posting;
    }

    public void setPosting(PostingInfo posting) {
        this.posting = posting;
    }

    public CommentInfo getComment() {
        return comment;
    }

    public void setComment(CommentInfo comment) {
        this.comment = comment;
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

    public String getRemoteMediaId() {
        return remoteMediaId;
    }

    public void setRemoteMediaId(String remoteMediaId) {
        this.remoteMediaId = remoteMediaId;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
