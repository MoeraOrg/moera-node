package org.moera.node.liberin.model;

import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class PostingCommentTotalsUpdatedLiberin extends Liberin {

    private Posting posting;
    private int total;

    public PostingCommentTotalsUpdatedLiberin(Posting posting, int total) {
        this.posting = posting;
        this.total = total;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
