package org.moera.node.model;

import org.moera.lib.node.types.RemotePostingVerificationInfo;
import org.moera.node.data.RemotePostingVerification;

public class RemotePostingVerificationInfoUtil {
    
    public static RemotePostingVerificationInfo build(RemotePostingVerification data) {
        RemotePostingVerificationInfo info = new RemotePostingVerificationInfo();
        info.setId(data.getId().toString());
        info.setNodeName(data.getNodeName());
        info.setPostingId(data.getPostingId());
        info.setRevisionId(data.getRevisionId());
        info.setStatus(data.getStatus());
        info.setErrorCode(data.getErrorCode());
        info.setErrorMessage(data.getErrorMessage());
        return info;
    }

}
