package org.moera.node.plugin;

import java.util.Objects;
import java.util.UUID;

public class PluginKey {

    private final UUID nodeId;
    private final String name;

    public PluginKey(UUID nodeId, String name) {
        this.nodeId = nodeId;
        this.name = name;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        PluginKey pluginKey = (PluginKey) peer;
        return Objects.equals(nodeId, pluginKey.nodeId) && name.equals(pluginKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, name);
    }

}
