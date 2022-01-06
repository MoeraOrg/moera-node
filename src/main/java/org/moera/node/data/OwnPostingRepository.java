package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OwnPostingRepository extends JpaRepository<OwnPosting, UUID> {

    @Query("select p from OwnPosting p where p.nodeId = ?1 and p.remoteNodeName = ?2 and p.remotePostingId = ?3")
    Optional<OwnPosting> findByRemotePostingId(UUID nodeId, String remoteNodeName, String remotePostingId);

}
