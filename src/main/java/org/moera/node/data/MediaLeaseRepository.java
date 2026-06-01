package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaLeaseRepository extends JpaRepository<MediaLease, UUID> {

    @Query("select ml from MediaLease ml where ml.nodeId = ?1 and ml.id = ?2")
    Optional<MediaLease> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query(
        "select count(*) from MediaLease ml where ml.nodeId = ?1 and ml.ownerName = ?2 and ml.mediaFileOwner.id = ?3"
    )
    int countByOwnerNameAndMediaId(UUID nodeId, String ownerName, UUID mediaId);

    @Query(
        "update MediaLease ml set ml.draftOnly = false, ml.deadline = null"
        + " where ml.nodeId = ?1 and ml.id in (?2)"
    )
    @Modifying
    void clearDraftOnly(UUID nodeId, Collection<UUID> ids);

    @Query(
        "delete from MediaLease ml"
        + " where ml.draftOnly = true and ml.deadline is not null and ml.deadline < ?1"
        + " and not exists ("
        + "     select ea from EntryAttachment ea"
        + "     where ea.mediaFileLease = ml and ea.draft is not null"
        + " )"
    )
    @Modifying
    void deleteExpiredDraftOnlyUnused(Timestamp deadline);

    @Query(
        "select ml from MediaLease ml"
        + " where ml.draftOnly = true and ml.deadline is not null and ml.deadline < ?1"
        + " and exists ("
        + "     select ea from EntryAttachment ea"
        + "     where ea.mediaFileLease = ml and ea.draft is not null"
        + " )"
    )
    List<MediaLease> findExpiredDraftOnlyUsed(Timestamp deadline);

}
