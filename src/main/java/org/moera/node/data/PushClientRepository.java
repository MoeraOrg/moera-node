package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PushClientRepository extends JpaRepository<PushClient, UUID> {

    @Query("select pc from PushClient pc where pc.nodeId = ?1")
    Collection<PushClient> findAllByNodeId(UUID nodeId);

    @Query("select pc from PushClient pc where pc.nodeId = ?1 and pc.clientId = ?2")
    Optional<PushClient> findByClientId(UUID nodeId, String clientId);

    @Query("update PushClient pc set pc.lastSeenAt = ?2 where pc.id = ?1")
    @Modifying
    void updateLastSeenAt(UUID id, Timestamp lastSeenAt);

}
