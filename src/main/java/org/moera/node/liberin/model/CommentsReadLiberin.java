package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class CommentsReadLiberin extends Liberin {

    private UUID postingId;
    private Long before;
    private Long after;
    private Integer limit;

    public CommentsReadLiberin(UUID postingId, Long before, Long after, Integer limit) {
        this.postingId = postingId;
        this.before = before;
        this.after = after;
        this.limit = limit;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
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
        model.put("postingId", postingId);
        model.put("before", before);
        model.put("after", after);
        model.put("limit", limit);
    }

}
