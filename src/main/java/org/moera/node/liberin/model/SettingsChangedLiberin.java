package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class SettingsChangedLiberin extends Liberin {

    private boolean nodeChanged;
    private boolean clientChanged;

    public SettingsChangedLiberin(boolean nodeChanged, boolean clientChanged) {
        this.nodeChanged = nodeChanged;
        this.clientChanged = clientChanged;
    }

    public boolean isNodeChanged() {
        return nodeChanged;
    }

    public void setNodeChanged(boolean nodeChanged) {
        this.nodeChanged = nodeChanged;
    }

    public boolean isClientChanged() {
        return clientChanged;
    }

    public void setClientChanged(boolean clientChanged) {
        this.clientChanged = clientChanged;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeChanged", nodeChanged);
        model.put("clientChanged", clientChanged);
    }

}
