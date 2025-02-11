package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.VerificationStatus;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.liberin.Liberin;

public class RemotePostingVerifiedLiberin extends Liberin {

    private RemotePostingVerification data;

    public RemotePostingVerifiedLiberin(RemotePostingVerification data) {
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
        model.put("correct", data.getStatus() == VerificationStatus.CORRECT);
    }

}
