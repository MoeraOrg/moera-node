package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "stories")
public class Story {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 63)
    private String feedName;

    @NotNull
    @Enumerated
    private StoryType storyType = StoryType.POSTING_ADDED;

    @ManyToOne
    private Story parent;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp publishedAt = Util.now();

    @NotNull
    private boolean pinned;

    @NotNull
    private Long moment;

    @NotNull
    private boolean viewed;

    @NotNull
    private boolean read;

    @NotNull
    private String summary = "";

    @NotNull
    private UUID trackingId;

    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteFullName;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @Size(max = 63)
    private String remoteOwnerName;

    @Size(max = 96)
    private String remoteOwnerFullName;

    @ManyToOne
    private MediaFile remoteOwnerAvatarMediaFile;

    @Size(max = 8)
    private String remoteOwnerAvatarShape;

    @Size(max = 40)
    private String remotePostingId;

    @Size(max = 40)
    private String remoteCommentId;

    @Size(max = 255)
    private String remoteHeading;

    @Size(max = 40)
    private String remoteRepliedToId;

    @Size(max = 255)
    private String remoteRepliedToHeading;

    @ManyToOne
    private Entry entry;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    private Set<Story> substories = new HashSet<>();

    public Story() {
    }

    public Story(UUID id, UUID nodeId, StoryType storyType) {
        this.id = id;
        this.nodeId = nodeId;
        this.storyType = storyType;
        this.trackingId = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
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

    public Story getParent() {
        return parent;
    }

    public void setParent(Story parent) {
        this.parent = parent;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
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

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public UUID getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(UUID trackingId) {
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

    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
    }

    public String getRemoteAvatarShape() {
        return remoteAvatarShape;
    }

    public void setRemoteAvatarShape(String remoteAvatarShape) {
        this.remoteAvatarShape = remoteAvatarShape;
    }

    public String getRemoteOwnerName() {
        return remoteOwnerName;
    }

    public void setRemoteOwnerName(String remoteOwnerName) {
        this.remoteOwnerName = remoteOwnerName;
    }

    public String getRemoteOwnerFullName() {
        return remoteOwnerFullName;
    }

    public void setRemoteOwnerFullName(String remoteOwnerFullName) {
        this.remoteOwnerFullName = remoteOwnerFullName;
    }

    public MediaFile getRemoteOwnerAvatarMediaFile() {
        return remoteOwnerAvatarMediaFile;
    }

    public void setRemoteOwnerAvatarMediaFile(MediaFile remoteOwnerAvatarMediaFile) {
        this.remoteOwnerAvatarMediaFile = remoteOwnerAvatarMediaFile;
    }

    public String getRemoteOwnerAvatarShape() {
        return remoteOwnerAvatarShape;
    }

    public void setRemoteOwnerAvatarShape(String remoteOwnerAvatarShape) {
        this.remoteOwnerAvatarShape = remoteOwnerAvatarShape;
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

    public String getRemoteHeading() {
        return remoteHeading;
    }

    public void setRemoteHeading(String remoteHeading) {
        this.remoteHeading = remoteHeading;
    }

    public String getRemoteRepliedToId() {
        return remoteRepliedToId;
    }

    public void setRemoteRepliedToId(String remoteRepliedToId) {
        this.remoteRepliedToId = remoteRepliedToId;
    }

    public String getRemoteRepliedToHeading() {
        return remoteRepliedToHeading;
    }

    public void setRemoteRepliedToHeading(String remoteRepliedToHeading) {
        this.remoteRepliedToHeading = remoteRepliedToHeading;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public Set<Story> getSubstories() {
        return substories;
    }

    public void setSubstories(Set<Story> substories) {
        this.substories = substories;
    }

    public void addSubstory(Story substory) {
        substories.add(substory);
        substory.setParent(this);
    }

    public void removeSubstory(Story substory) {
        substories.removeIf(sr -> sr.getId().equals(substory.getId()));
        substory.setParent(null);
    }

}
