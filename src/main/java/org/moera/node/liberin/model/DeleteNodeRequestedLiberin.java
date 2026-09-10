package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class DeleteNodeRequestedLiberin extends Liberin {

    private String message;

    public DeleteNodeRequestedLiberin(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
