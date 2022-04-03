package org.moera.node.liberin.model;

import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

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

}
