package org.moera.node.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.RemoteFeed;
import org.moera.lib.node.types.RemotePosting;
import org.moera.lib.node.types.SubscriptionType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionFilter {

    private SubscriptionType type;
    private Set<RemoteFeed> feeds;
    private Set<RemotePosting> postings;

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public Set<RemoteFeed> getFeeds() {
        return feeds;
    }

    public void setFeeds(Set<RemoteFeed> feeds) {
        this.feeds = feeds;
    }

    public Set<RemotePosting> getPostings() {
        return postings;
    }

    public void setPostings(Set<RemotePosting> postings) {
        this.postings = postings;
    }

}
