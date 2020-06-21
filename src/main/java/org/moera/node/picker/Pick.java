package org.moera.node.picker;

import org.moera.node.data.EntrySource;

public class Pick {

    private String remoteNodeName;
    private String remoteFeedName;
    private String remotePostingId;
    private String feedName;

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

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public boolean isSame(EntrySource entrySource) {
        return entrySource.getRemoteFeedName().equals(remoteFeedName)
                && entrySource.getRemoteNodeName().equals(remoteNodeName)
                && entrySource.getRemotePostingId().equals(remotePostingId);
    }

    public void toEntrySource(EntrySource entrySource) {
        entrySource.setRemoteFeedName(remoteFeedName);
        entrySource.setRemoteNodeName(remoteNodeName);
        entrySource.setRemotePostingId(remotePostingId);
    }

}
