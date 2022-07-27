package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;

public class CommentUpdatedLiberin extends Liberin {

    private Comment comment;
    private EntryRevision latestRevision;
    private Principal latestViewE;

    public CommentUpdatedLiberin(Comment comment, EntryRevision latestRevision, Principal latestViewE) {
        this.comment = comment;
        this.latestRevision = latestRevision;
        this.latestViewE = latestViewE;
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

    public Principal getLatestViewE() {
        return latestViewE;
    }

    public void setLatestViewE(Principal latestViewE) {
        this.latestViewE = latestViewE;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("comment", new CommentInfo(comment, AccessCheckers.ADMIN));
        model.put("latestRevision", new CommentRevisionInfo(comment, latestRevision, AccessCheckers.ADMIN));
        model.put("latestViewPrincipal", latestViewE);
    }

}
