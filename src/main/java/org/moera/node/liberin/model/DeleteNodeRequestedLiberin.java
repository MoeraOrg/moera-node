package org.moera.node.liberin.model;

import java.util.Map;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("message", message);
    }

}
