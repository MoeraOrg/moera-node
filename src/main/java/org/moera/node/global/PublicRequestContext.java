package org.moera.node.global;

public class PublicRequestContext implements RequestContext {

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public void setAdmin(boolean admin) {
    }

}
