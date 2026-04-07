package org.moera.node.userlist;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.lib.node.types.SubscriptionReason;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.task.Jobs;
import org.springframework.stereotype.Component;

@Component
public class UserListOperations {

    public static final Duration ABSENT_TTL = Duration.ofDays(3);
    public static final Duration PRESENT_TTL = Duration.ofDays(30);

    @Inject
    private UniversalContext universalContext;

    @Inject
    private RemoteUserListItemRepository remoteUserListItemRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    private Jobs jobs;

    public void addToList(String listNodeName, String listName, String nodeName) {
        RemoteUserListItem item = remoteUserListItemRepository
            .findByListAndNodeName(universalContext.nodeId(), listNodeName, listName, nodeName)
            .orElse(null);
        if (item != null) {
            if (item.isAbsent()) {
                item.setAbsent(false);
                item.setDeadline(Timestamp.from(Instant.now().plus(PRESENT_TTL)));
            }
        } else {
            item = new RemoteUserListItem();
            item.setId(UUID.randomUUID());
            item.setNodeId(universalContext.nodeId());
            item.setListNodeName(listNodeName);
            item.setListName(listName);
            item.setNodeName(nodeName);
            item.setAbsent(false);
            item.setDeadline(Timestamp.from(Instant.now().plus(PRESENT_TTL)));
            remoteUserListItemRepository.save(item);
        }
    }

    public void deleteFromList(String listNodeName, String listName, String nodeName) {
        RemoteUserListItem item = remoteUserListItemRepository
            .findByListAndNodeName(universalContext.nodeId(), listNodeName, listName, nodeName)
            .orElse(null);
        if (item == null || item.isAbsent()) {
            return;
        }
        item.setAbsent(true);
        item.setDeadline(Timestamp.from(Instant.now().plus(ABSENT_TTL)));
    }

    public void subscribeToList(String listNodeName, String listName, List<String> feedNames) {
        subscriptionOperations.subscribe(subscription -> {
            subscription.setSubscriptionType(SubscriptionType.USER_LIST);
            subscription.setRemoteNodeName(listNodeName);
            subscription.setRemoteFeedName(listName);
            subscription.setReason(SubscriptionReason.USER);
        });

        jobs.run(
            UserListUpdateJob.class,
            new UserListUpdateJob.Parameters(
                listNodeName,
                listName,
                feedNames,
                null,
                false
            ),
            universalContext.nodeId()
        );
    }

    public void unsubscribeFromList(String listNodeName, String listName, List<String> feedNames) {
        var userSubscriptions = userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
            universalContext.nodeId(), SubscriptionType.USER_LIST, listNodeName, listName
        );
        userSubscriptionRepository.deleteAll(userSubscriptions);

        jobs.run(
            UserListUpdateJob.class,
            new UserListUpdateJob.Parameters(
                listNodeName,
                listName,
                feedNames,
                null,
                true
            ),
            universalContext.nodeId()
        );
    }

}
