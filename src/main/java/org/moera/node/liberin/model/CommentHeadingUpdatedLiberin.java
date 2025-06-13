package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class CommentHeadingUpdatedLiberin extends Liberin {

    private UUID commentId;
    private UUID revisionId;
    private String heading;

    public CommentHeadingUpdatedLiberin(UUID commentId, UUID revisionId, String heading) {
        this.commentId = commentId;
        this.revisionId = revisionId;
        this.heading = heading;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("commentId", commentId);
        model.put("revisionId", revisionId);
        model.put("heading", heading);
    }

}
