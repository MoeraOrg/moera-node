package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfoUtil;

public class CommentDeletedLiberin extends Liberin {

    private Comment comment;
    private EntryRevision latestRevision;

    public CommentDeletedLiberin(Comment comment, EntryRevision latestRevision) {
        this.comment = comment;
        this.latestRevision = latestRevision;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public EntryRevision getLatestRevision() {
        return latestRevision;
    }

    public void setLatestRevision(EntryRevision latestRevision) {
        this.latestRevision = latestRevision;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        comment = entityManager.merge(comment);
        latestRevision = entityManager.merge(latestRevision);
        model.put("comment", CommentInfoUtil.build(comment, AccessCheckers.ADMIN));
    }

}
