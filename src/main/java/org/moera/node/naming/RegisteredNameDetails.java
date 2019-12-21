package org.moera.node.naming;

import org.moera.naming.rpc.RegisteredNameInfo;

public class RegisteredNameDetails implements Cloneable {

    private boolean latest;
    private String nodeUri;
    private byte[] signingKey;

    public RegisteredNameDetails() {
    }

    public RegisteredNameDetails(boolean latest, String nodeUri, byte[] signingKey) {
        this.latest = latest;
        this.nodeUri = nodeUri;
        this.signingKey = signingKey;
    }

    public RegisteredNameDetails(RegisteredNameInfo info) {
        latest = info.isLatest();
        nodeUri = info.getNodeUri();
        signingKey = info.getSigningKey();
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

    public byte[] getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(byte[] signingKey) {
        this.signingKey = signingKey;
    }

    @Override
    public RegisteredNameDetails clone() {
        return new RegisteredNameDetails(latest, nodeUri, signingKey);
    }

}
