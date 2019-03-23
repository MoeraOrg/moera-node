package org.moera.node.global;

public class PublicRequestContext implements RequestContext {

    @Override
    public boolean isBrowserExtension() {
        return false;
    }

    @Override
    public void setBrowserExtension(boolean browserExtension) {
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public void setAdmin(boolean admin) {
    }

}
