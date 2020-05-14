package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3"
            + " and s.feedName = ?4")
    int countByFeedName(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType, String feedName);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3"
            + " and s.entry.id = ?4")
    int countByEntryId(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType, UUID entryId);

}
