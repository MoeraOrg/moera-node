package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.WhoAmI;
import org.moera.node.liberin.Liberin;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeInfo", nodeInfo);
    }

}
