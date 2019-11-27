package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemotePostingVerificationRepository extends JpaRepository<RemotePostingVerification, UUID> {

    Optional<RemotePostingVerification> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("delete from RemotePostingVerification v where v.deadline < ?1")
    @Modifying
    void deleteOutdated(Timestamp deadline);

}
