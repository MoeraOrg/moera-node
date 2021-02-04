package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RemoteReactionVerificationRepository extends JpaRepository<RemoteReactionVerification, UUID> {

    Optional<RemoteReactionVerification> findByNodeIdAndId(UUID nodeId, UUID id);

}
