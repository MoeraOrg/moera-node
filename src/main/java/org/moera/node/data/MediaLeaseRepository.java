package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaLeaseRepository extends JpaRepository<MediaLease, UUID> {

    @Query("select ml from MediaLease ml where ml.nodeId = ?1 and ml.id = ?2")
    Optional<MediaLease> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query(
        "select count(*) from MediaLease ml where ml.nodeId = ?1 and ml.ownerName = ?2 and ml.mediaFileOwner.id = ?3"
    )
    int countByOwnerNameAndMediaId(UUID nodeId, String ownerName, UUID mediaId);

}
