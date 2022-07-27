package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.ReactionInfo;

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
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("comment", new CommentInfo(comment, AccessCheckers.ADMIN));
        model.put("reaction", new ReactionInfo(reaction, AccessCheckers.ADMIN));
    }

}
