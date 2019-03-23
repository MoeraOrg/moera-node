package org.moera.node.global;

public interface RequestContext {

    boolean isBrowserExtension();

    void setBrowserExtension(boolean browserExtension);

    boolean isAdmin();

    void setAdmin(boolean admin);

}
