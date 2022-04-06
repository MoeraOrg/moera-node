package org.moera.node.liberin.model;

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

}
