package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.model.StorySummaryData;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "stories")
public class Story {

    private static final Logger log = LoggerFactory.getLogger(Story.class);

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 63)
    private String feedName;

    @NotNull
    @Enumerated
    private StoryType storyType = StoryType.POSTING_ADDED;

    @ManyToOne(fetch = FetchType.LAZY)
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
    private boolean satisfied;

    @NotNull
    private String summary = "";

    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteFullName;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @Size(max = 63)
    private String remotePostingNodeName;

    @Size(max = 96)
    private String remotePostingFullName;

    @ManyToOne
    private MediaFile remotePostingAvatarMediaFile;

    @Size(max = 8)
    private String remotePostingAvatarShape;

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

    @Size(max = 40)
    private String remoteRepliedToId;

    @Size(max = 40)
    private String remoteParentPostingId;

    @Size(max = 40)
    private String remoteParentCommentId;

    @Size(max = 40)
    private String remoteParentMediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Entry entry;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    private Set<Story> substories = new HashSet<>();

    public Story() {
    }

    public Story(UUID id, UUID nodeId, StoryType storyType) {
        this.id = id;
        this.nodeId = nodeId;
        this.storyType = storyType;
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

    public boolean isSatisfied() {
        return satisfied;
    }

    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Transient
    public StorySummaryData getSummaryData() {
        try {
            return new ObjectMapper().readValue(getSummary(), StorySummaryData.class);
        } catch (JsonProcessingException e) {
            log.error("Cannot decode story summary data: {}", getSummary());
            return null;
        }
    }

    @Transient
    public void setSummaryData(StorySummaryData summaryData) {
        try {
            setSummary(new ObjectMapper().writeValueAsString(summaryData));
        } catch (JsonProcessingException e) {
            log.error("Cannot encode story summary data: {}", summaryData);
        }
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

    public String getRemotePostingNodeName() {
        return remotePostingNodeName;
    }

    public void setRemotePostingNodeName(String remotePostingNodeName) {
        this.remotePostingNodeName = remotePostingNodeName;
    }

    public String getRemotePostingFullName() {
        return remotePostingFullName;
    }

    public void setRemotePostingFullName(String remotePostingFullName) {
        this.remotePostingFullName = remotePostingFullName;
    }

    public MediaFile getRemotePostingAvatarMediaFile() {
        return remotePostingAvatarMediaFile;
    }

    public void setRemotePostingAvatarMediaFile(MediaFile remotePostingAvatarMediaFile) {
        this.remotePostingAvatarMediaFile = remotePostingAvatarMediaFile;
    }

    public String getRemotePostingAvatarShape() {
        return remotePostingAvatarShape;
    }

    public void setRemotePostingAvatarShape(String remotePostingAvatarShape) {
        this.remotePostingAvatarShape = remotePostingAvatarShape;
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

    public String getRemoteRepliedToId() {
        return remoteRepliedToId;
    }

    public void setRemoteRepliedToId(String remoteRepliedToId) {
        this.remoteRepliedToId = remoteRepliedToId;
    }

    public String getRemoteParentPostingId() {
        return remoteParentPostingId;
    }

    public void setRemoteParentPostingId(String remoteParentPostingId) {
        this.remoteParentPostingId = remoteParentPostingId;
    }

    public String getRemoteParentCommentId() {
        return remoteParentCommentId;
    }

    public void setRemoteParentCommentId(String remoteParentCommentId) {
        this.remoteParentCommentId = remoteParentCommentId;
    }

    public String getRemoteParentMediaId() {
        return remoteParentMediaId;
    }

    public void setRemoteParentMediaId(String remoteParentMediaId) {
        this.remoteParentMediaId = remoteParentMediaId;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    @Transient
    public PrincipalFilter getViewPrincipalFilter() {
        if (getEntry() == null) {
            return Principal.PUBLIC;
        }
        Principal viewEntry = getEntry().getViewE();
        return getEntry().getParent() != null
                ? getEntry().getParent().getViewE().a()
                    .and(getEntry().getParent().getViewCommentsE())
                    .and(viewEntry)
                : viewEntry;
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
