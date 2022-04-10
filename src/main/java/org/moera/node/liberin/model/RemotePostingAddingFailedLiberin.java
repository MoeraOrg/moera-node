package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.WhoAmI;

public class RemotePostingAddingFailedLiberin extends Liberin {

    private WhoAmI nodeInfo;

    public RemotePostingAddingFailedLiberin(WhoAmI nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public WhoAmI getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(WhoAmI nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

}
