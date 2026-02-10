package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileOwnerRepository extends JpaRepository<MediaFileOwner, UUID> {

    @Query("select mo from MediaFileOwner mo left join fetch mo.mediaFile where mo.nodeId = ?1 and mo.id = ?2")
    Optional<MediaFileOwner> findFullById(UUID nodeId, UUID id);

    @Query("select mo from MediaFileOwner mo left join fetch mo.mediaFile where mo.nodeId = ?1 and mo.id in (?2)")
    Set<MediaFileOwner> findByIds(UUID nodeId, UUID[] ids);

    @Query("select mo from MediaFileOwner mo where mo.nodeId = ?1 and mo.ownerName is null and mo.mediaFile.id = ?2")
    Collection<MediaFileOwner> findByAdminFile(UUID nodeId, String mediaFileId);

    @Query("select mo from MediaFileOwner mo where mo.nodeId = ?1 and mo.ownerName = ?2 and mo.mediaFile.id = ?3")
    Collection<MediaFileOwner> findByFile(UUID nodeId, String ownerName, String mediaFileId);

    @Query("select mo from MediaFileOwner mo where mo.mediaFile.id = ?1")
    Collection<MediaFileOwner> findAllByFile(String mediaFileId);

    @Query("delete from MediaFileOwner mo where mo.deadline is not null and mo.deadline < ?1")
    @Modifying
    void deleteUnused(Timestamp deadline);

    @Query(
        "select mo from MediaFileOwner mo where mo.nodeId = ?1 and mo.ownerName is null"
        + " and not exists (select p from Posting p where p.parentMedia = mo)"
    )
    List<MediaFileOwner> findWithoutPosting(UUID nodeId);

    @Query("select mo from MediaFileOwner mo where mo.usageUpdatedAt > mo.permissionsUpdatedAt")
    Page<MediaFileOwner> findOutdatedPermissions(Pageable pageable);

    @Query(
        "update MediaFileOwner mo"
        + " set mo.usageUpdatedAt = ?3"
        + " where mo.id in ("
        + "select ca.mediaFileOwner.id"
        + " from Comment c full join c.revisions cr full join cr.attachments ca"
        + " where c.nodeId = ?1 and c.parent.id = ?2 and cr.deletedAt is null and c.deletedAt is null"
        + ")"
    )
    @Modifying
    void updateUsageOfCommentAttachments(UUID nodeId, UUID postingId, Timestamp now);

    @Query("select mo from MediaFileOwner mo where mo.nonce is null")
    Page<MediaFileOwner> findAllWithoutNonce(Pageable pageable);

    @Query(
        "update MediaFileOwner mo"
        + " set mo.prevNonce = mo.nonce, mo.nonce = ?2, mo.nonceDeadline = ?3"
        + " where mo.id = ?1"
    )
    @Modifying
    void replaceNonce(UUID id, String nonce, Timestamp nonceDeadline);

    @Query("select mo from MediaFileOwner mo where mo.nonceDeadline < ?1 and mo.viewPrincipal != 'public'")
    Page<MediaFileOwner> findOutdatedNonce(Timestamp now, Pageable pageable);

    @Query(value = "lock table only media_file_owners in exclusive mode", nativeQuery = true)
    @Modifying
    void lockExclusive();

}
