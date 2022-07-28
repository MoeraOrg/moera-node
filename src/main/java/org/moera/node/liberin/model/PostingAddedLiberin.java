package org.moera.node.liberin.model;

import java.util.Map;

import javax.persistence.EntityManager;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;

public class PostingAddedLiberin extends Liberin {

    private Posting posting;

    public PostingAddedLiberin(Posting posting) {
        this.posting = posting;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        posting = entityManager.merge(posting);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
    }

}
