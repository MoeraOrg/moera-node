package org.moera.node.liberin.model;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.liberin.Liberin;

public class CommentUpdatedLiberin extends Liberin {

    private Comment comment;
    private EntryRevision latestRevision;
    private Principal latestViewE;
    private boolean latestPremoderating;

    public CommentUpdatedLiberin(Comment comment, EntryRevision latestRevision, Principal latestViewE) {
        this(comment, latestRevision, latestViewE, false);
    }

    public CommentUpdatedLiberin(
        Comment comment,
        EntryRevision latestRevision,
        Principal latestViewE,
        boolean latestPremoderating
    ) {
        this.comment = comment;
        this.latestRevision = latestRevision;
        this.latestViewE = latestViewE;
        this.latestPremoderating = latestPremoderating;
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

    public boolean isLatestPremoderating() {
        return latestPremoderating;
    }

    public void setLatestPremoderating(boolean latestPremoderating) {
        this.latestPremoderating = latestPremoderating;
    }

}
