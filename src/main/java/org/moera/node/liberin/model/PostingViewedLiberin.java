package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class PostingViewedLiberin extends Liberin {

    private UUID postingId;
    private int viewCount;

    public PostingViewedLiberin(UUID postingId, int viewCount) {
        this.postingId = postingId;
        this.viewCount = viewCount;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("postingId", postingId);
        model.put("viewCount", viewCount);
    }

}
