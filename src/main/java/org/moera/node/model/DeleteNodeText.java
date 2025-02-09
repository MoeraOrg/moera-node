package org.moera.node.model;

import jakarta.validation.constraints.Size;

public class DeleteNodeText {

    @Size(max = 1024)
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
