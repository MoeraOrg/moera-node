package org.moera.node.model;

import org.moera.node.option.Options;

public class DeleteNodeStatus {

    private boolean requested;

    public DeleteNodeStatus() {
    }

    public DeleteNodeStatus(Options options) {
        requested = options.getBool("delete-node.requested");
    }

    public boolean isRequested() {
        return requested;
    }

    public void setRequested(boolean requested) {
        this.requested = requested;
    }

}
