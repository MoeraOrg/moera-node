package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.moera.lib.node.types.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("select s from Subscription s"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.remoteSubscriberId = ?3 and s.usageCount > 0")
    Optional<Subscription> findBySubscriber(UUID nodeId, String remoteNodeName, String remoteSubscriberId);

    @Query("select s from Subscription s where s.status = org.moera.node.data.SubscriptionStatus.PENDING")
    Collection<Subscription> findPending();

    @Query("select s from Subscription s where s.usageCount = 0")
    Collection<Subscription> findUnused();

    @Query("select s from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2")
    List<Subscription> findAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("update Subscription s set s.retryAt = ?2 where s.id = ?1")
    @Modifying
    void updateRetryAtById(UUID id, Timestamp retryAt);

    @Query("update Subscription s"
            + " set s.status = org.moera.node.data.SubscriptionStatus.ESTABLISHED, s.remoteSubscriberId = ?2"
            + " where s.id = ?1")
    @Modifying
    void updateRemoteSubscriberIdById(UUID id, String remoteSubscriberId);

}
