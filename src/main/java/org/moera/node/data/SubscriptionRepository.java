package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>,
        QuerydslPredicateExecutor<Subscription> {

    @Query("select s from Subscription s left join fetch s.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2")
    List<Subscription> findAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.remoteNodeName = ?3"
            + " and s.remoteEntryId = ?4")
    List<Subscription> findAllByTypeAndNodeAndEntryId(UUID nodeId, SubscriptionType subscriptionType,
                                                      String remoteNodeName, String remoteEntryId);

    @Query("delete Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.remoteNodeName = ?3"
            + " and s.remoteEntryId = ?4")
    @Modifying
    void deleteByTypeAndNodeAndEntryId(UUID nodeId, SubscriptionType subscriptionType,
                                       String remoteNodeName, String remoteEntryId);

    @Query("select s from Subscription s left join fetch s.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.remoteEntryId = ?3")
    List<Subscription> findAllByNodeAndEntryId(UUID nodeId, String remoteNodeName, String remoteEntryId);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2")
    int countByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from Subscription s where s.nodeId = ?1 and s.remoteNodeName = ?2 and s.remoteSubscriberId = ?3")
    Optional<Subscription> findBySubscriber(UUID nodeId, String remoteNodeName, String remoteSubscriberId);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2"
            + " and s.remoteNodeName = ?3 and s.remoteSubscriberId = ?4")
    int countBySubscriber(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName,
                          String remoteSubscriberId);

    @Query("select s from Subscription s left join fetch s.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.remoteEntryId in (?2)")
    List<Subscription> findAllByRemotePostingIds(UUID nodeId, List<String> remotePostingIds);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.remoteNodeName = ?2")
    int countByRemoteNode(UUID nodeId, String remoteNodeName);

    @Query("select count(*) from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2"
            + " and s.remoteNodeName = ?3")
    int countByTypeAndRemoteNode(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName);

    @Query("select s from Subscription s where s.nodeId = ?1 and s.subscriptionType = ?2"
            + " and s.remoteNodeName = ?3")
    Optional<Subscription> findByTypeAndRemoteNode(UUID nodeId, SubscriptionType subscriptionType,
                                                   String remoteNodeName);

    @Query("update Subscription s set s.remoteFullName = ?3 where s.nodeId = ?1 and s.remoteNodeName = ?2")
    @Modifying
    void updateRemoteFullName(UUID nodeId, String remoteNodeName, String remoteFullName);

    @Query("update Subscription s set s.remoteAvatarMediaFile = ?3, s.remoteAvatarShape = ?4"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2")
    @Modifying
    void updateRemoteAvatar(UUID nodeId, String remoteNodeName, MediaFile mediaFile, String shape);

}
