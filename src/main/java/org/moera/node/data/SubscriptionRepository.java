package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2")
    int countByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.remoteNodeName = ?3"
            + " and s.remoteSubscriberId = ?4")
    Optional<Subscription> findBySubscriber(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName,
                                            String remoteSubscriberId);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2"
            + " and s.remoteNodeName = ?3 and s.remoteSubscriberId = ?4")
    int countBySubscriber(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName,
                          String remoteSubscriberId);

}
