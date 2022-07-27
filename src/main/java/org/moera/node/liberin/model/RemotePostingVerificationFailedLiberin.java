package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.RemotePostingVerification;
import org.moera.node.liberin.Liberin;

public class RemotePostingVerificationFailedLiberin extends Liberin {

    private RemotePostingVerification data;

    public RemotePostingVerificationFailedLiberin(RemotePostingVerification data) {
        this.data = data;
    }

    public RemotePostingVerification getData() {
        return data;
    }

    public void setData(RemotePostingVerification data) {
        this.data = data;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("id", data.getId());
        model.put("nodeName", data.getNodeName());
        model.put("receiverName", data.getOwnerName());
        model.put("postingId", data.getPostingId());
        model.put("revisionId", data.getRevisionId());
        model.put("errorCode", data.getErrorCode());
        model.put("errorMessage", data.getErrorMessage());
    }

}
