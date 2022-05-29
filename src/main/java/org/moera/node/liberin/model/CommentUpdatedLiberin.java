package org.moera.node.liberin.model;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.liberin.Liberin;

public class CommentUpdatedLiberin extends Liberin {

    private Comment comment;
    private EntryRevision latestRevision;
    private Principal latestViewPrincipal;

    public CommentUpdatedLiberin(Comment comment, EntryRevision latestRevision, Principal latestViewPrincipal) {
        this.comment = comment;
        this.latestRevision = latestRevision;
        this.latestViewPrincipal = latestViewPrincipal;
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

    public Principal getLatestViewPrincipal() {
        return latestViewPrincipal;
    }

    public void setLatestViewPrincipal(Principal latestViewPrincipal) {
        this.latestViewPrincipal = latestViewPrincipal;
    }

}
