package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query("select p from Posting p where p.nodeId = ?1 and p.entryId = ?2 and p.deletedAt is null")
    Optional<Posting> findByEntryId(UUID nodeId, UUID entryId);

    @Query("select count(*) from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3"
            + " and p.deletedAt is null")
    int countInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select p.moment from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3 and"
            + " p.deletedAt is null")
    Page<Long> findMomentsInRange(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select p from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3 and p.deletedAt is null"
            + " order by p.moment desc")
    List<Posting> findInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select p from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3 and p.deletedAt is null")
    Page<Posting> findSlice(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select min(p.createdAt) from Posting p where p.nodeId = ?1 and p.entryId = ?2")
    Timestamp firstCreatedAt(UUID nodeId, UUID entryId);

    @Query("select count(*) from Posting p where p.nodeId = ?1 and p.entryId = ?2")
    int countRevisions(UUID nodeId, UUID entryId);

}
