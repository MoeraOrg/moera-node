package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.VerificationStatus;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.liberin.Liberin;

public class RemoteReactionVerifiedLiberin extends Liberin {

    private RemoteReactionVerification data;

    public RemoteReactionVerifiedLiberin(RemoteReactionVerification data) {
        this.data = data;
    }

    public RemoteReactionVerification getData() {
        return data;
    }

    public void setData(RemoteReactionVerification data) {
        this.data = data;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("id", data.getId());
        model.put("nodeName", data.getNodeName());
        model.put("postingId", data.getPostingId());
        model.put("commentId", data.getCommentId());
        model.put("reactionOwnerName", data.getOwnerName());
        model.put("correct", data.getStatus() == VerificationStatus.CORRECT);
    }

}
