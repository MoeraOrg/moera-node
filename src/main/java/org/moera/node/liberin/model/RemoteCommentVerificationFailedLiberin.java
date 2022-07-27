package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.liberin.Liberin;

public class RemoteCommentVerificationFailedLiberin extends Liberin {

    private RemoteCommentVerification data;

    public RemoteCommentVerificationFailedLiberin(RemoteCommentVerification data) {
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
        model.put("errorCode", data.getErrorCode());
        model.put("errorMessage", data.getErrorMessage());
    }

}
