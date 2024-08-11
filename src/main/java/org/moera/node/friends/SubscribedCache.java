package org.moera.node.friends;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.Nodes;
import org.moera.node.util.ParametrizedLock;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class SubscribedCache {

    private static final int CACHE_SIZE_MIN = 16;
    private static final int CACHE_SIZE_MAX = 1024; // TODO make configurable and find optimal value

    private final Map<Nodes, Boolean> cache = new ConcurrentHashMap<>();
    private final ParametrizedLock<Nodes> lock = new ParametrizedLock<>();
    private final UsageQueue<Nodes> usageQueue = new UsageQueue<>(CACHE_SIZE_MIN);

    @Inject
    private UniversalContext universalContext;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    public boolean isSubscribed(String remoteNodeName) {
        return isSubscribed(universalContext.nodeId(), remoteNodeName);
    }

    public boolean isSubscribed(UUID nodeId, String remoteNodeName) {
        if (nodeId == null || ObjectUtils.isEmpty(remoteNodeName)) {
            return false;
        }

        Nodes nodes = new Nodes(nodeId, remoteNodeName);
        Boolean subscribed = cache.get(nodes);
        if (subscribed == null) {
            lock.lock(nodes);
            try {
                subscribed = cache.computeIfAbsent(nodes,
                        nid -> userSubscriptionRepository.countByTypeAndRemoteNode(
                                nodeId, SubscriptionType.FEED, remoteNodeName) > 0);
            } finally {
                lock.unlock(nodes);
            }
        }
        updateUsage(nodes);

        return subscribed;
    }

    private void updateUsage(Nodes nodes) {
        Usage<Nodes> usage = new Usage<>(nodes);
        //noinspection ResultOfMethodCallIgnored
        usageQueue.remove(usage);
        usageQueue.add(usage);
        while (usageQueue.size() > CACHE_SIZE_MAX) {
            usage = usageQueue.poll();
            invalidate(usage.value.nodeId, usage.value.remoteNodeName);
        }
    }

    public void invalidate(UUID nodeId, String remoteNodeName) {
        Nodes nodeClient = new Nodes(nodeId, remoteNodeName);
        Usage<Nodes> usage = new Usage<>(nodeClient);
        //noinspection ResultOfMethodCallIgnored
        usageQueue.remove(usage);
        cache.remove(nodeClient);
    }

    public void invalidate(Nodes nodes) {
        invalidate(nodes.nodeId, nodes.remoteNodeName);
    }

}
