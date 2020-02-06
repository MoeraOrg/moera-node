package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query("select p from Posting p join fetch p.currentRevision left join fetch p.reactionTotals"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is null and draft = false")
    Optional<Posting> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Posting p where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    int countInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select p.currentRevision.moment from Posting p where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    Page<Long> findMomentsInRange(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select p from Posting p left join fetch p.reactionTotals where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    Set<Posting> findInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query(value = "select p from Posting p left join fetch p.currentRevision where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null",
           countQuery = "select count(p) from Posting p where p.nodeId = ?1 and p.currentRevision.moment > ?2"
                   + " and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    Page<Posting> findSlice(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select p from Posting p where p.nodeId = ?1 and p.deletedAt is not null and draft = false")
    List<Posting> findDeleted(UUID nodeId, Pageable pageable);

    @Query("select p from Posting p join fetch p.currentRevision join fetch p.reactionTotals"
            + " where p.nodeId = ?1 and p.id = ?2 and p.deletedAt is not null and draft = false")
    Optional<Posting> findDeletedById(UUID nodeId, UUID id);

    @Query("select p from Posting p join fetch p.draftRevision"
            + " where p.nodeId = ?1 and p.deletedAt is null and draft = true")
    List<Posting> findDrafts(UUID nodeId, Pageable pageable);

    @Query("select p from Posting p join fetch p.draftRevision where p.nodeId = ?1 and p.id = ?2"
            + " and p.deletedAt is null and draft = true")
    Optional<Posting> findDraftById(UUID nodeId, UUID id);

    @Query("delete from Posting p where p.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadlineBefore);

}
