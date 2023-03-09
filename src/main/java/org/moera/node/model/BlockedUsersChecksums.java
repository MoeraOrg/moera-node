package org.moera.node.model;

public class BlockedUsersChecksums {

    private long visibility;

    public BlockedUsersChecksums() {
    }

    public BlockedUsersChecksums(long visibility) {
        this.visibility = visibility;
    }

    public long getVisibility() {
        return visibility;
    }

    public void setVisibility(long visibility) {
        this.visibility = visibility;
    }

}
