package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface BlockedUserRepository
        extends JpaRepository<BlockedUser, UUID>, QuerydslPredicateExecutor<BlockedUser> {

    @Query("select count(distinct bu.remoteNodeName) from BlockedUser bu where bu.nodeId = ?1")
    int countByNodeId(UUID nodeId);

    @Query("select bu from BlockedUser bu left join fetch bu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bu.nodeId = ?1 and bu.id = ?2")
    Optional<BlockedUser> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select bu from BlockedUser bu left join fetch bu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bu.nodeId = ?1 and bu.blockedOperation = ?2 and bu.remoteNodeName = ?3 and bu.entry is null"
            + " and bu.entryNodeName is null")
    Collection<BlockedUser> findByOperationAndNodeAndNoEntry(
            UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName);

    @Query("select bu from BlockedUser bu left join fetch bu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bu.nodeId = ?1 and bu.blockedOperation = ?2 and bu.remoteNodeName = ?3 and bu.entry.id = ?4")
    Collection<BlockedUser> findByOperationAndNodeAndEntry(
            UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, UUID entryId);

    @Query("select bu from BlockedUser bu left join fetch bu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bu.nodeId = ?1 and bu.blockedOperation = ?2 and bu.remoteNodeName = ?3 and bu.entryNodeName = ?4"
            + " and bu.entryPostingId = ?5")
    Collection<BlockedUser> findByOperationAndNodeAndRemoteEntry(
            UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, String entryNodeName,
            String entryPostingId);

    @Query("select bu from BlockedUser bu where bu.deadline < ?1")
    Collection<BlockedUser> findExpired(Timestamp deadline);

}
