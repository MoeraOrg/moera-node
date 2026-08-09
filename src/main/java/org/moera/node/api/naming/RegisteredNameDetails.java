package org.moera.node.api.naming;

import org.moera.lib.naming.NodeName;
import org.moera.lib.naming.types.RegisteredNameInfo;

public record RegisteredNameDetails(
    String nodeName,
    String nodeUri,
    byte[] signingKey
) implements Cloneable {

    public RegisteredNameDetails() {
        this(null, null, null);
    }

    public RegisteredNameDetails(RegisteredNameInfo info) {
        this(NodeName.toString(info.getName(), info.getGeneration()), info.getNodeUri(), info.getSigningKey());
    }

    @Override
    public RegisteredNameDetails clone() {
        return new RegisteredNameDetails(nodeName, nodeUri, signingKey);
    }

}
