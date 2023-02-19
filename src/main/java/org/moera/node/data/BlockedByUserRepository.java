package org.moera.node.data;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface BlockedByUserRepository
        extends JpaRepository<BlockedByUser, UUID>, QuerydslPredicateExecutor<BlockedByUser> {

    @Query("select count(distinct bbu.remoteNodeName) from BlockedByUser bbu where bbu.nodeId = ?1")
    int countByNodeId(UUID nodeId);

    @Query("select bbu from BlockedByUser bbu left join fetch bbu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bbu.nodeId = ?1 and bbu.id = ?2")
    Optional<BlockedByUser> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select bbu from BlockedByUser bbu left join fetch bbu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bbu.nodeId = ?1 and bbu.remoteNodeName = ?2 and bbu.remotePostingId is null")
    Collection<BlockedByUser> findByRemoteNode(UUID nodeId, String remoteNodeName);

    @Query("select bbu from BlockedByUser bbu left join fetch bbu.contact c left join fetch c.remoteAvatarMediaFile"
            + " where bbu.nodeId = ?1 and bbu.remoteNodeName = ?2 and bbu.remotePostingId = ?3")
    Collection<BlockedByUser> findByRemotePosting(UUID nodeId, String remoteNodeName, String remotePostingId);

}
