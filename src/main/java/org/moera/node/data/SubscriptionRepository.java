package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>,
        QuerydslPredicateExecutor<Subscription> {

    @Query("select s from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2")
    List<Subscription> findAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2")
    int countByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscription s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.remoteSubscriberId = ?3")
    Optional<Subscription> findBySubscriber(UUID nodeId, String remoteNodeName, String remoteSubscriberId);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2"
            + " and s.remoteNodeName = ?3 and s.remoteSubscriberId = ?4")
    int countBySubscriber(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName,
                          String remoteSubscriberId);

}
