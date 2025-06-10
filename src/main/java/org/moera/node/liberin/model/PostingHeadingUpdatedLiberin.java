package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class PostingHeadingUpdatedLiberin extends Liberin {

    private UUID postingId;
    private UUID revisionId;
    private String heading;
    private String description;

    public PostingHeadingUpdatedLiberin(UUID postingId, UUID revisionId, String heading, String description) {
        this.postingId = postingId;
        this.revisionId = revisionId;
        this.heading = heading;
        this.description = description;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("postingId", postingId);
        model.put("revisionId", revisionId);
        model.put("heading", heading);
        model.put("description", description);
    }

}
