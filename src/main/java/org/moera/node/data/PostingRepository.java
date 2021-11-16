package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query("select p from Posting p where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is null")
    Optional<Posting> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select p from Posting p"
            + " join fetch p.currentRevision cr left join fetch cr.attachments cra"
            + " left join fetch cra.mediaFileOwner mfo left join fetch mfo.mediaFile mf left join fetch mf.previews"
            + " left join fetch p.reactionTotals left join fetch p.sources left join fetch p.ownerAvatarMediaFile"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is null")
    Optional<Posting> findFullByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select p from Posting p"
            + " join fetch p.currentRevision cr left join fetch cr.attachments cra"
            + " left join fetch cra.mediaFileOwner mfo left join fetch mfo.mediaFile mf left join fetch mf.previews"
            + " where p.nodeId = ?1 and p.deletedAt is not null")
    List<Posting> findDeleted(UUID nodeId, Pageable pageable);

    @Query("select p from Posting p"
            + " join fetch p.currentRevision cr left join fetch cr.attachments cra"
            + " left join fetch cra.mediaFileOwner mfo left join fetch mfo.mediaFile mf left join fetch mf.previews"
            + " join fetch p.reactionTotals"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is not null")
    Optional<Posting> findDeletedById(UUID nodeId, UUID id);

    @Query("delete from Posting p where p.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadlineBefore);

    @Query("select p from Posting p where p.nodeId = ?1 and p.receiverName = ?2 and p.receiverEntryId = ?3")
    Optional<Posting> findByReceiverId(UUID nodeId, String receiverName, String receiverEntryId);

    @Query("select p from Posting p where p.nodeId = ?1 and p.deletedAt is null"
            + " and not exists(select s from Story s where s.entry.id = p.id)")
    List<Posting> findUnlinked(UUID nodeId);

}
