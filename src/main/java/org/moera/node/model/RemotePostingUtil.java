package org.moera.node.model;

import org.moera.lib.node.types.RemotePosting;

public class RemotePostingUtil {

    public static RemotePosting build(String nodeName, String postingId) {
        RemotePosting remotePosting = new RemotePosting();
        remotePosting.setNodeName(nodeName);
        remotePosting.setPostingId(postingId);
        return remotePosting;
    }

}
