package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.EntrySource;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingSourceInfo {

    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private String feedName;
    private String postingId;
    private Long createdAt;

    public PostingSourceInfo() {
    }

    public PostingSourceInfo(EntrySource entrySource) {
        nodeName = entrySource.getRemoteNodeName();
        fullName = entrySource.getRemoteFullName();
        if (entrySource.getRemoteAvatarMediaFile() != null) {
            avatar = AvatarImageUtil.build(entrySource.getRemoteAvatarMediaFile(), entrySource.getRemoteAvatarShape());
        }
        feedName = entrySource.getRemoteFeedName();
        postingId = entrySource.getRemotePostingId();
        createdAt = Util.toEpochSecond(entrySource.getCreatedAt());
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

}
