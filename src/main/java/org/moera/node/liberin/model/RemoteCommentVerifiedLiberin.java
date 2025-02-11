package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.VerificationStatus;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.liberin.Liberin;

public class RemoteCommentVerifiedLiberin extends Liberin {

    private RemoteCommentVerification data;

    public RemoteCommentVerifiedLiberin(RemoteCommentVerification data) {
        this.data = data;
    }

    public RemoteCommentVerification getData() {
        return data;
    }

    public void setData(RemoteCommentVerification data) {
        this.data = data;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("id", data.getId());
        model.put("nodeName", data.getNodeName());
        model.put("postingId", data.getPostingId());
        model.put("commentId", data.getCommentId());
        model.put("correct", data.getStatus() == VerificationStatus.CORRECT);
    }

}
