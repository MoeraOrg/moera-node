package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SubscriberRepository extends JpaRepository<Subscriber, UUID>, QuerydslPredicateExecutor<Subscriber> {

    @Query("select s from Subscriber s left join fetch s.remoteAvatarMediaFile where s.nodeId = ?1 and s.id = ?2")
    Optional<Subscriber> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.subscriptionType = ?2")
    int countAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscriber s left join fetch s.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2")
    List<Subscriber> findAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2"
            + " and s.subscriptionType = ?3")
    int countByType(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3"
            + " and s.feedName = ?4")
    int countByFeedName(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType, String feedName);

    @Query("select s from Subscriber s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.feedName = ?3")
    List<Subscriber> findAllByFeedName(UUID nodeId, SubscriptionType subscriptionType, String feedName);

    @Query("select count(*) from Subscriber s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.subscriptionType = ?3"
            + " and s.entry.id = ?4")
    int countByEntryId(UUID nodeId, String remoteNodeName, SubscriptionType subscriptionType, UUID entryId);

    @Query("select s from Subscriber s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.entry.id = ?3")
    List<Subscriber> findAllByEntryId(UUID nodeId, SubscriptionType subscriptionType, UUID entryId);

    @Query("update Subscriber s set s.remoteFullName = ?3, s.remoteGender = ?4"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2")
    @Modifying
    void updateRemoteFullNameAndGender(UUID nodeId, String remoteNodeName, String remoteFullName, String remoteGender);

    @Query("update Subscriber s set s.remoteAvatarMediaFile = ?3, s.remoteAvatarShape = ?4"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2")
    @Modifying
    void updateRemoteAvatar(UUID nodeId, String remoteNodeName, MediaFile mediaFile, String shape);

}
