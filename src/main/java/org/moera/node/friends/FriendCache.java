package org.moera.node.friends;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final int CLIENT_GROUPS_CACHE_SIZE_MIN = 16;
    private static final int CLIENT_GROUPS_CACHE_SIZE_MAX = 1024; // TODO make configurable and find optimal value

    private final Map<UUID, FriendGroup[]> nodeGroups = new ConcurrentHashMap<>();
    private final ParametrizedLock<UUID> nodeGroupsLock = new ParametrizedLock<>();

    private final Map<Nodes, Friend[]> clientGroups = new ConcurrentHashMap<>();
    private final ParametrizedLock<Nodes> clientGroupsLock = new ParametrizedLock<>();
    private final UsageQueue<Nodes> clientUsageQueue = new UsageQueue<>(CLIENT_GROUPS_CACHE_SIZE_MIN);

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

    public String[] getNodeGroupIds() {
        FriendGroup[] groups = getNodeGroups();
        return groups != null
                ? Arrays.stream(groups)
                    .map(FriendGroup::getId)
                    .map(UUID::toString)
                    .toArray(String[]::new)
                : null;
    }

    public Optional<FriendGroup> getNodeGroup(UUID id) {
        return Arrays.stream(getNodeGroups())
                .filter(fg -> fg.getId().equals(id))
                .findFirst();
    }

    public Friend[] getClientGroups(String clientName) {
        if (ObjectUtils.isEmpty(clientName)) {
            return null;
        }

        Nodes nodeClient = new Nodes(universalContext.nodeId(), clientName);
        Friend[] groups = clientGroups.get(nodeClient);
        if (groups == null) {
            clientGroupsLock.lock(nodeClient);
            try {
                groups = clientGroups.computeIfAbsent(nodeClient,
                        nid -> fetchClientGroups(clientName).toArray(Friend[]::new));
            } finally {
                clientGroupsLock.unlock(nodeClient);
            }
        }
        updateUsage(nodeClient);

        return groups;
    }

    private List<Friend> fetchClientGroups(String clientName) {
        if (Objects.equals(universalContext.nodeName(), clientName)) {
            return friendRepository.findByNodeId(universalContext.nodeId());
        } else {
            return friendRepository.findByNodeIdAndName(universalContext.nodeId(), clientName);
        }
    }

    private void updateUsage(Nodes nodeClient) {
        Usage<Nodes> usage = new Usage<>(nodeClient);
        //noinspection ResultOfMethodCallIgnored
        clientUsageQueue.remove(usage);
        clientUsageQueue.add(usage);
        while (clientUsageQueue.size() > CLIENT_GROUPS_CACHE_SIZE_MAX) {
            usage = clientUsageQueue.poll();
            invalidateClientGroups(usage.value.nodeId, usage.value.remoteNodeName);
        }
    }

    public String[] getClientGroupIds(String clientName) {
        Friend[] groups = getClientGroups(clientName);
        return groups != null
                ? Arrays.stream(groups)
                    .map(f -> f.getFriendGroup().getId())
                    .map(UUID::toString)
                    .toArray(String[]::new)
                : null;
    }

    public void invalidateNodeGroups(UUID nodeId) {
        nodeGroups.remove(nodeId);
    }

    public void invalidateClientGroups(UUID nodeId) {
        List<Nodes> keys = clientGroups.keySet().stream()
                .filter(key -> key.nodeId.equals(nodeId))
                .toList();
        keys.forEach(clientGroups::remove);
    }

    public void invalidateClientGroups(UUID nodeId, String clientName) {
        Nodes nodeClient = new Nodes(nodeId, clientName);
        Usage<Nodes> usage = new Usage<>(nodeClient);
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
