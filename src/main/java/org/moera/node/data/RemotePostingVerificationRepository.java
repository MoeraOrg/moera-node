package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RemotePostingVerificationRepository extends JpaRepository<RemotePostingVerification, UUID> {

    Optional<RemotePostingVerification> findByNodeIdAndId(UUID nodeId, UUID id);

}
