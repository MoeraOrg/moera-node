package org.moera.node.model;

import java.util.List;

public class GrantChange {

    private List<String> scope;

    private boolean revoke;

    public GrantChange() {
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public boolean isRevoke() {
        return revoke;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }

}
