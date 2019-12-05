package org.moera.node.model;

import java.util.UUID;

public class AsyncOperationCreated {

    private String id;

    public AsyncOperationCreated() {
    }

    public AsyncOperationCreated(UUID id) {
        this.id = id.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
