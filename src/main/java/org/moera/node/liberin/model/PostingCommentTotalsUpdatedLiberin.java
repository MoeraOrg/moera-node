package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        model.put("total", total);
    }

}
