package org.moera.node.model;

import org.moera.node.data.BlockedOperation;

public class BlockedByUserFilter {

    private BlockedOperation[] blockedOperations;
    private RemotePosting[] postings;

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

}
