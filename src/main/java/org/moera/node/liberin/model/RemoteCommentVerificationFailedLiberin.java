package org.moera.node.liberin.model;

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

}
