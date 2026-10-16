package org.moera.node.liberin.model;

import java.util.List;

import org.moera.node.data.Comment;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;

public class CommentReactionTotalsUpdatedLiberin extends Liberin {

    private Comment comment;
    private List<Reaction> addedReactions = List.of();
    private List<Reaction> deletedReactions = List.of();

    public CommentReactionTotalsUpdatedLiberin(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public List<Reaction> getAddedReactions() {
        return addedReactions;
    }

    public void setAddedReactions(List<Reaction> addedReactions) {
        this.addedReactions = addedReactions;
    }

    public List<Reaction> getDeletedReactions() {
        return deletedReactions;
    }

    public void setDeletedReactions(List<Reaction> deletedReactions) {
        this.deletedReactions = deletedReactions;
    }

}
