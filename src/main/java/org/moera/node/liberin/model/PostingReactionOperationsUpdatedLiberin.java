package org.moera.node.liberin.model;

import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;

public class PostingReactionOperationsUpdatedLiberin extends Liberin {

    private Posting posting;
    private Reaction reaction;

    public PostingReactionOperationsUpdatedLiberin(Posting posting, Reaction reaction) {
        this.posting = posting;
        this.reaction = reaction;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
    }

}
