package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class NodeSettingsMetadataChangedLiberin extends Liberin {

    public NodeSettingsMetadataChangedLiberin(UUID nodeId) {
        setNodeId(nodeId);
    }

}
