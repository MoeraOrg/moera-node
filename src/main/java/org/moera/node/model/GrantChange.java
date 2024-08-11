package org.moera.node.model;

public class GrantChange {

    private long scope;

    private boolean revoke;

    public GrantChange() {
    }

    public long getScope() {
        return scope;
    }

    public void setScope(long scope) {
        this.scope = scope;
    }

    public boolean isRevoke() {
        return revoke;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }

}
