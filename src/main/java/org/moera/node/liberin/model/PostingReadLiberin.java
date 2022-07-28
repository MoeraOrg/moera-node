package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class PostingReadLiberin extends Liberin {

    private UUID id;

    public PostingReadLiberin(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("id", id);
    }

}
