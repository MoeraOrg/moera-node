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

    @Query("select p from Posting p"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is null and p.draft = false")
    Optional<Posting> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select p from Posting p"
            + " join fetch p.currentRevision left join fetch p.reactionTotals left join fetch p.sources"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is null and p.draft = false")
    Optional<Posting> findFullByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select p from Posting p where p.nodeId = ?1 and p.deletedAt is not null and p.draft = false")
    List<Posting> findDeleted(UUID nodeId, Pageable pageable);

    @Query("select p from Posting p join fetch p.currentRevision join fetch p.reactionTotals"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is not null and p.draft = false")
    Optional<Posting> findDeletedById(UUID nodeId, UUID id);

    @Query("select p from Posting p join fetch p.draftRevision"
            + " where p.nodeId = ?1 and p.deletedAt is null and p.draft = true")
    List<Posting> findDrafts(UUID nodeId, Pageable pageable);

    @Query("select p from Posting p join fetch p.draftRevision where p.nodeId = ?1 and p.id = ?2"
            + " and p.deletedAt is null and p.draft = true")
    Optional<Posting> findDraftById(UUID nodeId, UUID id);

    @Query("delete from Posting p where p.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadlineBefore);

    @Query("select p from Posting p where p.nodeId = ?1 and p.receiverName = ?2 and p.receiverEntryId = ?3")
    Optional<Posting> findByReceiverId(UUID nodeId, String receiverName, String receiverEntryId);

}
