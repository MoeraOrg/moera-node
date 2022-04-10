package org.moera.node.liberin.model;

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

}