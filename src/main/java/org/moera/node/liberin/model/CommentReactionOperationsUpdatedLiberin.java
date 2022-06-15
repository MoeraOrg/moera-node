package org.moera.node.liberin.model;

import org.moera.node.data.Comment;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;

public class CommentReactionOperationsUpdatedLiberin extends Liberin {

    private Comment comment;
    private Reaction reaction;

    public CommentReactionOperationsUpdatedLiberin(Comment comment, Reaction reaction) {
        this.comment = comment;
        this.reaction = reaction;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
    }

}
