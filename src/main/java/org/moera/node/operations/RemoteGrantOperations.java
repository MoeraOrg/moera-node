package org.moera.node.operations;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.RemoteGrant;
import org.moera.node.data.RemoteGrantRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.Nodes;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class RemoteGrantOperations {

    private final ParametrizedLock<Nodes> grantsLock = new ParametrizedLock<>();

    @Inject
    private UniversalContext universalContext;

    @Inject
    private RemoteGrantRepository remoteGrantRepository;

    public long get(String remoteNodeName) {
        RemoteGrant remoteGrant =
                remoteGrantRepository.findByNodeName(universalContext.nodeId(), remoteNodeName).orElse(null);
        return remoteGrant != null ? remoteGrant.getAuthScope() : 0;
    }

    public void put(String remoteNodeName, long scope) {
        Nodes key = new Nodes(universalContext.nodeId(), remoteNodeName);
        grantsLock.lock(key);
        try {
            RemoteGrant remoteGrant =
                    remoteGrantRepository.findByNodeName(universalContext.nodeId(), remoteNodeName).orElse(null);
            if (remoteGrant == null) {
                if (scope == 0) {
                    return;
                }

                remoteGrant = new RemoteGrant();
                remoteGrant.setId(UUID.randomUUID());
                remoteGrant.setNodeId(universalContext.nodeId());
                remoteGrant.setRemoteNodeName(remoteNodeName);
                remoteGrant = remoteGrantRepository.save(remoteGrant);
            } else if (scope == 0) {
                remoteGrantRepository.delete(remoteGrant);
                return;
            }
            remoteGrant.setAuthScope(scope);
            remoteGrant.setUpdatedAt(Util.now());
        } finally {
            grantsLock.unlock(key);
        }
    }

}
