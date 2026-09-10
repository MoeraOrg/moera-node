package org.moera.node.liberin.model;

import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class CommentAddedLiberin extends Liberin {

    private Posting posting;
    private Comment comment;

    public CommentAddedLiberin(Posting posting, Comment comment) {
        this.posting = posting;
        this.comment = comment;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

}
