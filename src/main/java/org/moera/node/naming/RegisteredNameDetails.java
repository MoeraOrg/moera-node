package org.moera.node.naming;

import org.moera.naming.rpc.RegisteredNameInfo;

public class RegisteredNameDetails implements Cloneable {

    private String nodeName;
    private boolean latest;
    private String nodeUri;
    private byte[] signingKey;

    public RegisteredNameDetails() {
    }

    public RegisteredNameDetails(String nodeName, boolean latest, String nodeUri, byte[] signingKey) {
        this.nodeName = nodeName;
        this.latest = latest;
        this.nodeUri = nodeUri;
        this.signingKey = signingKey;
    }

    public RegisteredNameDetails(RegisteredNameInfo info) {
        nodeName = RegisteredName.toString(info.getName(), info.getGeneration());
        latest = info.isLatest();
        nodeUri = info.getNodeUri();
        signingKey = info.getSigningKey();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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
        return new RegisteredNameDetails(nodeName, latest, nodeUri, signingKey);
    }

}
