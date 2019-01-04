package org.moera.node.model;

public class CredentialsCreated {

    private boolean created;

    public CredentialsCreated() {
    }

    public CredentialsCreated(boolean created) {
        this.created = created;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

}
