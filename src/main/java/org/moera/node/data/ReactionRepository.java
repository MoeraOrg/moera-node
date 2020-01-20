package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.ownerName = ?2 and r.deletedAt is null")
    Reaction findByEntryIdAndOwner(UUID entryId, String ownerName);

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.ownerName = ?2"
            + " and r.deletedAt is not null")
    List<Reaction> findDeletedByEntryIdAndOwner(UUID entryId, String ownerName, Pageable pageable);

    @Query("select r from Reaction r left join r.entryRevision where r.deadline < ?1")
    List<Reaction> findExpired(Timestamp deadline);

    @Query("select r from Reaction r left join r.entryRevision.entry p"
            + " where p.nodeId = ?1 and p.currentRevision.moment > ?2 and p.currentRevision.moment <= ?3"
            + " and p.deletedAt is null and r.ownerName = ?4 and r.deletedAt is null")
    List<Reaction> findByEntriesInRangeAndOwner(UUID nodeId, long afterMoment, long beforeMoment, String ownerName);

    @Query("select count(*) from Reaction r where r.entryRevision.entry.id = ?1 and r.moment = ?2")
    int countMoments(UUID entryId, long moment);

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.negative = ?2"
            + " and r.moment > ?3 and r.moment <= ?4 and r.deletedAt is null")
    Page<Reaction> findSlice(UUID postingId, boolean negative, long afterMoment, long beforeMoment, Pageable pageable);

}
