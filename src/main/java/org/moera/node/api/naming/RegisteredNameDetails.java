package org.moera.node.api.naming;

import org.moera.lib.naming.NodeName;
import org.moera.lib.naming.types.RegisteredNameInfo;

public class RegisteredNameDetails implements Cloneable {

    private String nodeName;
    private String nodeUri;
    private byte[] signingKey;

    public RegisteredNameDetails() {
    }

    public RegisteredNameDetails(String nodeName, String nodeUri, byte[] signingKey) {
        this.nodeName = nodeName;
        this.nodeUri = nodeUri;
        this.signingKey = signingKey;
    }

    public RegisteredNameDetails(RegisteredNameInfo info) {
        nodeName = NodeName.toString(info.getName(), info.getGeneration());
        nodeUri = info.getNodeUri();
        signingKey = info.getSigningKey();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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
        return new RegisteredNameDetails(nodeName, nodeUri, signingKey);
    }

}
