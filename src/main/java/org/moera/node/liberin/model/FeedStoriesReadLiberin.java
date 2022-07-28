package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class FeedStoriesReadLiberin extends Liberin {

    private String feedName;
    private Long before;
    private Long after;
    private Integer limit;

    public FeedStoriesReadLiberin(String feedName, Long before, Long after, Integer limit) {
        this.feedName = feedName;
        this.before = before;
        this.after = after;
        this.limit = limit;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Long getBefore() {
        return before;
    }

    public void setBefore(Long before) {
        this.before = before;
    }

    public Long getAfter() {
        return after;
    }

    public void setAfter(Long after) {
        this.after = after;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("feedName", feedName);
        model.put("before", before);
        model.put("after", after);
        model.put("limit", limit);
    }

}
