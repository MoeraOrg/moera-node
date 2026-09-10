package org.moera.node.liberin.model;

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

}
