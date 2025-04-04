package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfoUtil;
import org.moera.node.model.ReactionInfoUtil;

public class CommentReactionDeletedLiberin extends Liberin {

    private Comment comment;
    private Reaction reaction;

    public CommentReactionDeletedLiberin(Comment comment) {
        this.comment = comment;
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

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        comment = entityManager.merge(comment);
        reaction = entityManager.merge(reaction);
        model.put("comment", CommentInfoUtil.build(comment, AccessCheckers.ADMIN));
        model.put("reaction", ReactionInfoUtil.build(reaction, AccessCheckers.ADMIN));
    }

}
