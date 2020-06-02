package org.moera.node.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedInfo implements Cloneable {

    private String feedName;
    private String subscriberId;
    private Map<String, String[]> operations;

    public FeedInfo() {
    }

    public FeedInfo(String feedName, String subscriberId, Map<String, String[]> operations) {
        this.feedName = feedName;
        this.subscriberId = subscriberId;
        this.operations = operations;
    }

    public FeedInfo(String feedName) {
        this.feedName = feedName;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

    @Override
    public FeedInfo clone() {
        return new FeedInfo(feedName, subscriberId, operations);
    }

}
