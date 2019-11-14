package org.moera.node.naming;

import org.moera.naming.rpc.RegisteredNameInfo;

public class RegisteredNameDetails {

    private boolean latest;
    private String nodeUri;

    public RegisteredNameDetails(boolean latest, String nodeUri) {
        this.latest = latest;
        this.nodeUri = nodeUri;
    }

    public RegisteredNameDetails(RegisteredNameInfo info) {
        latest = info.isLatest();
        nodeUri = info.getNodeUri();
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    public String getNodeUri() {
        return nodeUri;
    }

    public void setNodeUri(String nodeUri) {
        this.nodeUri = nodeUri;
    }

}
