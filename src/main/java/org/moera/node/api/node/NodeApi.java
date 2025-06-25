package org.moera.node.api.node;

import jakarta.inject.Inject;

import org.moera.lib.node.MoeraNode;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.api.naming.RegisteredNameDetails;
import org.moera.node.util.UriUtil;
import org.springframework.stereotype.Service;

@Service
public class NodeApi {

    @Inject
    private NamingCache namingCache;

    private String fetchNodeUri(String remoteNodeName) {
        RegisteredNameDetails details = namingCache.get(remoteNodeName);
        return details != null ? UriUtil.normalize(details.getNodeUri()) : null;
    }

    public MoeraNode at(String remoteNodeName) throws MoeraNodeUnknownNameException {
        String nodeUri = fetchNodeUri(remoteNodeName);
        if (nodeUri == null) {
            throw new MoeraNodeUnknownNameException(remoteNodeName);
        }
        return new MoeraNode(nodeUri);
    }

    public MoeraNode at(String remoteNodeName, String carte) throws MoeraNodeUnknownNameException {
        MoeraNode node = at(remoteNodeName);
        if (carte != null) {
            node.carte(carte);
            node.auth();
        }
        return node;
    }

}
