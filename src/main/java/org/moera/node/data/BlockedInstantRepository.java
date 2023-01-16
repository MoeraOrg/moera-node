package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface BlockedInstantRepository
        extends JpaRepository<BlockedInstant, UUID>, QuerydslPredicateExecutor<BlockedInstant> {

    @Query("select bi from BlockedInstant bi where bi.nodeId = ?1 and bi.id = ?2")
    Optional<BlockedInstant> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("delete from BlockedInstant bi where bi.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
