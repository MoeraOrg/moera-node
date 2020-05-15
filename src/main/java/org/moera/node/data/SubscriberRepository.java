package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {

    @Query("select s from Subscriber s where s.nodeId = ?1 and s.id = ?2")
    Optional<Subscriber> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3"
            + " and s.feedName = ?4")
    int countByFeedName(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType, String feedName);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3"
            + " and s.entry.id = ?4")
    int countByEntryId(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType, UUID entryId);

    @Query("select s from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3")
    List<Subscriber> findByType(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType);

}
