package org.moera.node.liberin.model;

import java.util.Map;

import javax.persistence.EntityManager;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;

public class PostingDeletedLiberin extends Liberin {

    private Posting posting;
    private EntryRevision latestRevision;

    public PostingDeletedLiberin(Posting posting, EntryRevision latestRevision) {
        this.posting = posting;
        this.latestRevision = latestRevision;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public EntryRevision getLatestRevision() {
        return latestRevision;
    }

    public void setLatestRevision(EntryRevision latestRevision) {
        this.latestRevision = latestRevision;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        posting = entityManager.merge(posting);
        latestRevision = entityManager.merge(latestRevision);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        model.put("latestRevision",
                new PostingRevisionInfo(posting, latestRevision, posting.getReceiverName(), AccessCheckers.ADMIN));
    }

}
