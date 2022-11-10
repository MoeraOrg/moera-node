package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class FeaturesUpdatedLiberin extends Liberin {

    private String clientName;

    public FeaturesUpdatedLiberin() {
    }

    public FeaturesUpdatedLiberin(String clientName) {
        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("clientName", clientName);
    }

}
