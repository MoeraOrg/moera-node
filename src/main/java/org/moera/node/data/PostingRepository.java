package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query("select p from Posting p left join p.currentRevision where p.nodeId = ?1 and p.id = ?2"
            + " and p.deletedAt is null")
    Optional<Posting> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Posting p where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    int countInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select p.currentRevision.moment from Posting p where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    Page<Long> findMomentsInRange(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select p from Posting p where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null"
            + " order by p.currentRevision.moment desc")
    List<Posting> findInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select p from Posting p where p.nodeId = ?1"
            + " and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3 and p.deletedAt is null")
    Page<Posting> findSlice(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select p from Posting p where p.nodeId = ?1 and p.deletedAt is not null")
    List<Posting> findDeleted(UUID nodeId, Pageable pageable);

}
