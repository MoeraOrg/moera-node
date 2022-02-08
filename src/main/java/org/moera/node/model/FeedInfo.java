package org.moera.node.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedInfo implements Cloneable {

    private String feedName;
    private String title;
    private String subscriberId;
    private int total;
    private Long firstCreatedAt;
    private Long lastCreatedAt;
    private Map<String, String[]> operations;

    public FeedInfo() {
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Long getFirstCreatedAt() {
        return firstCreatedAt;
    }

    public void setFirstCreatedAt(Long firstCreatedAt) {
        this.firstCreatedAt = firstCreatedAt;
    }

    public Long getLastCreatedAt() {
        return lastCreatedAt;
    }

    public void setLastCreatedAt(Long lastCreatedAt) {
        this.lastCreatedAt = lastCreatedAt;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

    @Override
    public FeedInfo clone() {
        try {
            return (FeedInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Must implement Cloneable", e);
        }
    }

}
