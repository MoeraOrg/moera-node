package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.util.Util;

@Entity
@Table(name = "picks")
public class Pick {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 63)
    private String feedName;

    @NotNull
    @Size(max = 135)
    private String remoteNodeName = "";

    @Size(max = 63)
    private String remoteFeedName;

    @NotNull
    @Size(max = 40)
    private String remotePostingId;

    @Size(max = 135)
    private String sourceNodeName;

    @Size(max = 40)
    private String sourcePostingId;

    @ManyToOne
    private MediaFileOwner mediaFileOwner;

    @ManyToOne
    private RemoteMediaFile remoteMediaFile;

    @ManyToOne
    private Entry parentMediaEntry;

    private boolean recommended;

    private boolean pinned;

    private boolean viewed;

    private boolean read;

    private boolean satisfied;

    private Principal postingViewPrincipal;

    private boolean allowMultiple;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp publishAt;

    private Timestamp retryAt;

    @Transient
    private boolean running;

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

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getSourceNodeName() {
        return sourceNodeName;
    }

    public void setSourceNodeName(String sourceNodeName) {
        this.sourceNodeName = sourceNodeName;
    }

    public String getSourcePostingId() {
        return sourcePostingId;
    }

    public void setSourcePostingId(String sourcePostingId) {
        this.sourcePostingId = sourcePostingId;
    }

    public MediaFileOwner getMediaFileOwner() {
        return mediaFileOwner;
    }

    public void setMediaFileOwner(MediaFileOwner mediaFileOwner) {
        this.mediaFileOwner = mediaFileOwner;
    }

    public RemoteMediaFile getRemoteMediaFile() {
        return remoteMediaFile;
    }

    public void setRemoteMediaFile(RemoteMediaFile remoteMediaFile) {
        this.remoteMediaFile = remoteMediaFile;
    }

    public Entry getParentMediaEntry() {
        return parentMediaEntry;
    }

    public void setParentMediaEntry(Entry parentMediaEntry) {
        this.parentMediaEntry = parentMediaEntry;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
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

    public Principal getPostingViewPrincipal() {
        return postingViewPrincipal;
    }

    public void setPostingViewPrincipal(Principal postingViewPrincipal) {
        this.postingViewPrincipal = postingViewPrincipal;
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public void setAllowMultiple(boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Timestamp publishAt) {
        this.publishAt = publishAt;
    }

    public Timestamp getRetryAt() {
        return retryAt;
    }

    public void setRetryAt(Timestamp retryAt) {
        this.retryAt = retryAt;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Pick redirectTo(String remoteNodeName, String remotePostingId) {
        Pick pick = new Pick();
        pick.feedName = feedName;
        pick.remoteFeedName = remoteFeedName;
        pick.recommended = recommended;
        pick.pinned = pinned;
        pick.viewed = viewed;
        pick.read = read;
        pick.satisfied = satisfied;
        pick.postingViewPrincipal = postingViewPrincipal;
        pick.allowMultiple = allowMultiple;
        pick.publishAt = publishAt;
        pick.sourceNodeName = sourceNodeName != null ? sourceNodeName : this.remoteNodeName;
        pick.sourcePostingId = sourcePostingId != null ? sourcePostingId : this.remotePostingId;
        pick.remoteNodeName = remoteNodeName;
        pick.remotePostingId = remotePostingId;
        return pick;
    }

    public boolean isSame(EntrySource entrySource) {
        return entrySource.getRemoteFeedName().equals(remoteFeedName)
                && entrySource.getRemoteNodeName().equals(remoteNodeName)
                && entrySource.getRemotePostingId().equals(remotePostingId);
    }

}
