package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class PostingRecommendationUpdatedLiberin extends Liberin {

    private UUID postingId;

    public PostingRecommendationUpdatedLiberin(UUID postingId) {
        this.postingId = postingId;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

}
