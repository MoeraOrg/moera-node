package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

public class FeedInfo {

    private String feedName;
    private Map<String, String[]> operations;

    public FeedInfo() {
    }

    public FeedInfo(String feedName) {
        this.feedName = feedName;
        operations = Collections.singletonMap("add", new String[]{"admin"});
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
