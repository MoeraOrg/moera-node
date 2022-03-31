package org.moera.node.liberin.model;

import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class PostingRestoredLiberin extends Liberin {

    private Posting posting;

    public PostingRestoredLiberin(Posting posting) {
        this.posting = posting;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

}
