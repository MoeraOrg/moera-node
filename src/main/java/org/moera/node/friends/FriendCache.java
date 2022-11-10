package org.moera.node.friends;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
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

    private static class NodeClientUsage {

        public NodeClient nodeClient;
        public Instant usedAt = Instant.now();

        NodeClientUsage(NodeClient nodeClient) {
            this.nodeClient = nodeClient;
        }

        @Override
        public boolean equals(Object peer) {
            if (this == peer) {
                return true;
            }
            if (peer == null || getClass() != peer.getClass()) {
                return false;
            }
            NodeClientUsage nodeClientUsage = (NodeClientUsage) peer;
            return nodeClient.equals(nodeClientUsage.nodeClient);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeClient);
        }

    }

    private static final int CLIENT_GROUPS_CACHE_SIZE_MIN = 16;
    private static final int CLIENT_GROUPS_CACHE_SIZE_MAX = 1024; // TODO make configurable and find optimal value

    private final Map<UUID, FriendGroup[]> nodeGroups = new ConcurrentHashMap<>();
    private final ParametrizedLock<UUID> nodeGroupsLock = new ParametrizedLock<>();

    private final Map<NodeClient, FriendGroup[]> clientGroups = new ConcurrentHashMap<>();
    private final ParametrizedLock<NodeClient> clientGroupsLock = new ParametrizedLock<>();
    private final PriorityBlockingQueue<NodeClientUsage> clientUsageQueue = new PriorityBlockingQueue<>(
            CLIENT_GROUPS_CACHE_SIZE_MIN, Comparator.comparing(u -> u.usedAt));

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
        if (groups == null) {
            clientGroupsLock.lock(nodeClient);
            try {
                groups = clientGroups.computeIfAbsent(nodeClient,
                        nid -> friendRepository.findAllByNodeIdAndName(universalContext.nodeId(), clientName).stream()
                                .map(Friend::getFriendGroup)
                                .toArray(FriendGroup[]::new));
            } finally {
                clientGroupsLock.unlock(nodeClient);
            }
        }
        updateUsage(nodeClient);

        return groups;
    }

    private void updateUsage(NodeClient nodeClient) {
        NodeClientUsage usage = new NodeClientUsage(nodeClient);
        //noinspection ResultOfMethodCallIgnored
        clientUsageQueue.remove(usage);
        clientUsageQueue.add(usage);
        while (clientUsageQueue.size() > CLIENT_GROUPS_CACHE_SIZE_MAX) {
            usage = clientUsageQueue.poll();
            invalidateClientGroups(usage.nodeClient.nodeId, usage.nodeClient.clientName);
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

    public void invalidateNodeGroups(UUID nodeId) {
        nodeGroups.remove(nodeId);
    }

    public void invalidateClientGroups(UUID nodeId) {
        List<NodeClient> keys = clientGroups.keySet().stream()
                .filter(key -> key.nodeId.equals(nodeId))
                .collect(Collectors.toList());
        keys.forEach(clientGroups::remove);
    }

    public void invalidateClientGroups(UUID nodeId, String clientName) {
        NodeClient nodeClient = new NodeClient(nodeId, clientName);
        NodeClientUsage usage = new NodeClientUsage(nodeClient);
        //noinspection ResultOfMethodCallIgnored
        clientUsageQueue.remove(usage);
        clientGroups.remove(nodeClient);
    }

    public void invalidate(FriendCacheInvalidation invalidation) {
        switch (invalidation.getPart()) {
            case NODE_GROUPS:
                invalidateNodeGroups(invalidation.getNodeId());
                break;
            case CLIENT_GROUPS_ALL:
                invalidateClientGroups(invalidation.getNodeId());
                break;
            case CLIENT_GROUPS:
                invalidateClientGroups(invalidation.getNodeId(), invalidation.getClientName());
                break;
        }
    }

}
