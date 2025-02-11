package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.moera.lib.node.types.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SubscriberRepository extends JpaRepository<Subscriber, UUID>, QuerydslPredicateExecutor<Subscriber> {

    @Query("select s from Subscriber s left join fetch s.contact c left join fetch c.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.id = ?2")
    Optional<Subscriber> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.subscriptionType = ?2")
    int countAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscriber s left join fetch s.contact c left join fetch c.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2")
    List<Subscriber> findAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscriber s left join fetch s.contact c left join fetch c.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2 and s.feedName = ?3")
    List<Subscriber> findAllByFeedName(UUID nodeId, SubscriptionType subscriptionType, String feedName);

    @Query("select s from Subscriber s left join fetch s.contact c left join fetch c.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2 and s.entry.id = ?3")
    List<Subscriber> findAllByEntryId(UUID nodeId, SubscriptionType subscriptionType, UUID entryId);

}
