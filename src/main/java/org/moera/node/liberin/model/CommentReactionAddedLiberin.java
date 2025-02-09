package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.ReactionInfo;

public class CommentReactionAddedLiberin extends Liberin {

    private Comment comment;
    private Reaction addedReaction;
    private Reaction deletedReaction;

    public CommentReactionAddedLiberin(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Reaction getAddedReaction() {
        return addedReaction;
    }

    public void setAddedReaction(Reaction addedReaction) {
        this.addedReaction = addedReaction;
    }

    public Reaction getDeletedReaction() {
        return deletedReaction;
    }

    public void setDeletedReaction(Reaction deletedReaction) {
        this.deletedReaction = deletedReaction;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        comment = entityManager.merge(comment);
        model.put("comment", new CommentInfo(comment, AccessCheckers.ADMIN));
        if (addedReaction != null) {
            addedReaction = entityManager.merge(addedReaction);
            model.put("addedReaction", new ReactionInfo(addedReaction, AccessCheckers.ADMIN));
        }
        if (deletedReaction != null) {
            deletedReaction = entityManager.merge(deletedReaction);
            model.put("deletedReaction", new ReactionInfo(deletedReaction, AccessCheckers.ADMIN));
        }
    }

}
