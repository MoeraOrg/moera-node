package org.moera.node.liberin.model;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class PostingUpdatedLiberin extends Liberin {

    private Posting posting;
    private EntryRevision latestRevision;
    private Principal latestViewPrincipal;

    public PostingUpdatedLiberin(Posting posting, EntryRevision latestRevision, Principal latestViewPrincipal) {
        this.posting = posting;
        this.latestRevision = latestRevision;
        this.latestViewPrincipal = latestViewPrincipal;
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

    public Principal getLatestViewPrincipal() {
        return latestViewPrincipal;
    }

    public void setLatestViewPrincipal(Principal latestViewPrincipal) {
        this.latestViewPrincipal = latestViewPrincipal;
    }

}
