package org.moera.node.liberin.model;

import org.moera.node.data.Comment;
import org.moera.node.data.EntryRevision;
import org.moera.node.liberin.Liberin;

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

}
