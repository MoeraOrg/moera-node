package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteCommentVerificationRepository extends JpaRepository<RemoteCommentVerification, UUID> {

    Optional<RemoteCommentVerification> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("delete from RemoteCommentVerification v where v.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
