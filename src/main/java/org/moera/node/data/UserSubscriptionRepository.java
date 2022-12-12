package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID>,
        QuerydslPredicateExecutor<UserSubscription> {

    @Query("select s from UserSubscription s left join fetch s.remoteAvatarMediaFile where s.nodeId = ?1 and s.id = ?2")
    Optional<UserSubscription> findAllByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select s from UserSubscription s left join fetch s.remoteAvatarMediaFile"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2")
    List<UserSubscription> findAllByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select s from UserSubscription s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.remoteNodeName = ?3"
            + " and s.remoteEntryId = ?4")
    List<UserSubscription> findAllByTypeAndNodeAndEntryId(UUID nodeId, SubscriptionType subscriptionType,
                                                          String remoteNodeName, String remoteEntryId);

    @Query("select s from UserSubscription s where s.nodeId = ?1 and s.subscriptionType = ?2 and s.remoteNodeName = ?3"
            + " and s.remoteFeedName = ?4")
    List<UserSubscription> findAllByTypeAndNodeAndFeedName(UUID nodeId, SubscriptionType subscriptionType,
                                                           String remoteNodeName, String remoteFeedName);

    @Query("select count(*) from UserSubscription s where s.nodeId = ?1 and s.subscriptionType = ?2")
    int countByType(UUID nodeId, SubscriptionType subscriptionType);

    @Query("select count(*) from UserSubscription s where s.nodeId = ?1 and s.subscriptionType = ?2"
            + " and s.remoteNodeName = ?3")
    int countByTypeAndRemoteNode(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName);

    @Query("select count(*) from UserSubscription s"
            + " where s.nodeId = ?1 and s.subscriptionType = ?2 and s.remoteNodeName = ?3 and s.remoteEntryId = ?4")
    int countByTypeAndNodeAndEntryId(UUID nodeId, SubscriptionType subscriptionType, String remoteNodeName,
                                     String remoteEntryId);

    @Query("update UserSubscription s set s.remoteFullName = ?3, s.remoteGender = ?4"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2")
    @Modifying
    void updateRemoteFullNameAndGender(UUID nodeId, String remoteNodeName, String remoteFullName, String remoteGender);

    @Query("update UserSubscription s set s.remoteAvatarMediaFile = ?3, s.remoteAvatarShape = ?4"
            + " where s.nodeId = ?1 and s.remoteNodeName = ?2")
    @Modifying
    void updateRemoteAvatar(UUID nodeId, String remoteNodeName, MediaFile mediaFile, String shape);

}
