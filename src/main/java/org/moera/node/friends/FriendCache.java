package org.moera.node.friends;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.ParametrizedLock;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class FriendCache {

    private static class NodeClient {

        public UUID nodeId;
        public String clientName;

        NodeClient(UUID nodeId, String clientName) {
            this.nodeId = nodeId;
            this.clientName = clientName;
        }

        @Override
        public boolean equals(Object peer) {
            if (this == peer) {
                return true;
            }
            if (peer == null || getClass() != peer.getClass()) {
                return false;
            }
            NodeClient nodeClient = (NodeClient) peer;
            return nodeId.equals(nodeClient.nodeId) && clientName.equals(nodeClient.clientName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, clientName);
        }

    }

    private final ConcurrentMap<UUID, FriendGroup[]> nodeGroups = new ConcurrentHashMap<>();
    private final ParametrizedLock<UUID> nodeGroupsLock = new ParametrizedLock<>();
    private final ConcurrentMap<NodeClient, FriendGroup[]> clientGroups = new ConcurrentHashMap<>();
    private final ParametrizedLock<NodeClient> clientGroupsLock = new ParametrizedLock<>();

    @Inject
    private UniversalContext universalContext;

    @Inject
    private FriendGroupRepository friendGroupRepository;

    @Inject
    private FriendRepository friendRepository;

    public FriendGroup[] getNodeGroups() {
        UUID nodeId = universalContext.nodeId();
        FriendGroup[] groups = nodeGroups.get(nodeId);
        if (groups != null) {
            return groups;
        }
        nodeGroupsLock.lock(nodeId);
        try {
            return nodeGroups.computeIfAbsent(nodeId,
                    nid -> friendGroupRepository.findAllByNodeId(nid).toArray(FriendGroup[]::new));
        } finally {
            nodeGroupsLock.unlock(nodeId);
        }
    }

    public FriendGroup[] getClientGroups(String clientName) {
        if (ObjectUtils.isEmpty(clientName)) {
            return null;
        }

        NodeClient nodeClient = new NodeClient(universalContext.nodeId(), clientName);
        FriendGroup[] groups = clientGroups.get(nodeClient);
        if (groups != null) {
            return groups;
        }
        clientGroupsLock.lock(nodeClient);
        try {
            return clientGroups.computeIfAbsent(nodeClient,
                    nid -> friendRepository.findAllByNodeIdAndName(universalContext.nodeId(), clientName).stream()
                            .map(Friend::getFriendGroup)
                            .toArray(FriendGroup[]::new));
        } finally {
            clientGroupsLock.unlock(nodeClient);
        }
    }

    public String[] getClientGroupIds(String clientName) {
        FriendGroup[] groups = getClientGroups(clientName);
        return groups != null
                ? Arrays.stream(groups)
                    .map(FriendGroup::getId)
                    .map(UUID::toString)
                    .toArray(String[]::new)
                : null;
    }

}
