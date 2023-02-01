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

    @Query("select bu from BlockedUser bu where bu.deadline < ?1")
    Collection<BlockedUser> findExpired(Timestamp deadline);

}
