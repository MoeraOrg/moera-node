package org.moera.node.operations;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.inject.Inject;

import org.moera.node.data.Grant;
import org.moera.node.data.GrantRepository;
import org.moera.node.util.Nodes;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Util;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class GrantCache {

    private final Map<Nodes, Long> grants = new ConcurrentHashMap<>();
    private final ParametrizedLock<Nodes> grantsLock = new ParametrizedLock<>();

    @Inject
    private GrantRepository grantRepository;

    public long get(UUID nodeId, String nodeName) {
        Long scope = grants.get(new Nodes(nodeId, nodeName));
        return scope != null ? scope : 0;
    }

    public long grant(UUID nodeId, String nodeName, long scope) {
        Nodes key = new Nodes(nodeId, nodeName);
        try (var ignored = grantsLock.lock(key)) {
            Grant grant = null;
            Long currentScope = grants.get(key);
            if (currentScope != null) {
                grant = grantRepository.findByNodeName(nodeId, nodeName).orElse(null);
            } else {
                currentScope = 0L;
            }

            if (grant == null) {
                grant = new Grant();
                grant.setId(UUID.randomUUID());
                grant.setNodeId(nodeId);
                grant.setNodeName(nodeName);
            }

            currentScope |= scope;
            grants.put(key, currentScope);

            grant.setAuthScope(currentScope);
            grant.setUpdatedAt(Util.now());
            grantRepository.save(grant);

            return currentScope;
        }
    }

    public long revoke(UUID nodeId, String nodeName, long scope) {
        Nodes key = new Nodes(nodeId, nodeName);
        try (var ignored = grantsLock.lock(key)) {
            Long currentScope = grants.get(key);
            if (currentScope == null) {
                return 0;
            }

            Grant grant = grantRepository.findByNodeName(nodeId, nodeName).orElse(null);

            currentScope &= ~scope;
            if (currentScope != 0) {
                grants.put(key, currentScope);

                if (grant == null) {
                    grant = new Grant();
                    grant.setId(UUID.randomUUID());
                    grant.setNodeId(nodeId);
                    grant.setNodeName(nodeName);
                    grant = grantRepository.save(grant);
                }
                grant.setAuthScope(currentScope);
            } else {
                grants.remove(key);

                if (grant != null) {
                    grantRepository.delete(grant);
                }
            }

            return currentScope;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        grantRepository.findAll().forEach(grant ->
                grants.put(new Nodes(grant.getNodeId(), grant.getNodeName()), grant.getAuthScope()));
    }

}
