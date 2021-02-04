package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RemoteCommentVerificationRepository extends JpaRepository<RemoteCommentVerification, UUID> {

    Optional<RemoteCommentVerification> findByNodeIdAndId(UUID nodeId, UUID id);

}
