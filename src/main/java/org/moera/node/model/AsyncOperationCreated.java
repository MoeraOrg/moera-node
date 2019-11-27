package org.moera.node.model;

import java.util.UUID;

public class AsyncOperationCreated {

    private UUID id;

    public AsyncOperationCreated() {
    }

    public AsyncOperationCreated(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
