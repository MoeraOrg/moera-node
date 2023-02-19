package org.moera.node.model;

import org.moera.node.data.BlockedOperation;

public class BlockedByUserFilter {

    private BlockedOperation[] blockedOperations;
    private RemotePosting[] postings;
    private Boolean strict;

    public BlockedOperation[] getBlockedOperations() {
        return blockedOperations;
    }

    public void setBlockedOperations(BlockedOperation[] blockedOperations) {
        this.blockedOperations = blockedOperations;
    }

    public RemotePosting[] getPostings() {
        return postings;
    }

    public void setPostings(RemotePosting[] postings) {
        this.postings = postings;
    }

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

}
