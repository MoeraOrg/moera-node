package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RemotePostingVerificationRepository extends JpaRepository<RemotePostingVerification, UUID> {

    @Query("select v from RemotePostingVerification v where v.nodeId = ?1 and v.id = ?2")
    Optional<RemotePostingVerification> findByNodeIdAndId(UUID nodeId, UUID id);

}
