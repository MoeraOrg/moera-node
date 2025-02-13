package org.moera.node.model;

import org.moera.lib.node.types.RemoteReactionVerificationInfo;
import org.moera.node.data.RemoteReactionVerification;

public class RemoteReactionVerificationInfoUtil {
    
    public static RemoteReactionVerificationInfo build(RemoteReactionVerification data) {
        RemoteReactionVerificationInfo info = new RemoteReactionVerificationInfo();
        info.setId(data.getId().toString());
        info.setNodeName(data.getNodeName());
        info.setPostingId(data.getPostingId());
        info.setReactionOwnerName(data.getOwnerName());
        info.setStatus(data.getStatus());
        info.setErrorCode(data.getErrorCode());
        info.setErrorMessage(data.getErrorMessage());
        return info;
    }

}
