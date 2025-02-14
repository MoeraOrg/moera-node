package org.moera.node.model;

import org.moera.lib.node.types.RemotePostingOrNode;

public class RemotePostingOrNodeUtil {

    public static RemotePostingOrNode build(String nodeName, String postingId) {
        RemotePostingOrNode remotePostingOrNode = new RemotePostingOrNode();
        remotePostingOrNode.setNodeName(nodeName);
        remotePostingOrNode.setPostingId(postingId);
        return remotePostingOrNode;
    }

}
