package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.PostingRevisionInfoUtil;
import org.moera.node.operations.MediaAttachmentsProvider;

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
        model.put("posting", PostingInfoUtil.build(posting, AccessCheckers.ADMIN));
        model.put(
            "latestRevision",
            PostingRevisionInfoUtil.build(
                posting, latestRevision, MediaAttachmentsProvider.RELATIONS, posting.getReceiverName(),
                AccessCheckers.ADMIN
            )
        );
    }

}
