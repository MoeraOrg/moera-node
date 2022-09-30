package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    @Query("select r from Reaction r"
            + " left join fetch r.ownerAvatarMediaFile"
            + " where r.entryRevision.entry.id = ?1 and r.ownerName = ?2 and r.deletedAt is null")
    Reaction findByEntryIdAndOwner(UUID entryId, String ownerName);

    @Query("select r from Reaction r"
            + " left join fetch r.ownerAvatarMediaFile"
            + " where r.entryRevision.entry.id in (?1) and r.ownerName = ?2 and r.deletedAt is null")
    List<Reaction> findByEntryIdsAndOwner(Collection<UUID> entryIds, String ownerName);

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.ownerName = ?2"
            + " and r.deletedAt is not null and r.replaced = true")
    List<Reaction> findReplacedByEntryIdAndOwner(UUID entryId, String ownerName, Pageable pageable);

    @Query("select r from Reaction r left join r.entryRevision where r.deadline < ?1")
    List<Reaction> findExpired(Timestamp deadline);

    @Query("select r from Reaction r"
            + " left join fetch r.ownerAvatarMediaFile"
            + " left join fetch r.entryRevision er left join fetch er.entry p left join p.stories s"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4"
            + " and r.ownerName = ?5 and r.deletedAt is null")
    Set<Reaction> findByStoriesInRangeAndOwner(UUID nodeId, String feedName, long afterMoment, long beforeMoment,
                                               String ownerName);

    @Query("select r from Reaction r"
            + " left join fetch r.ownerAvatarMediaFile"
            + " left join fetch r.entryRevision er left join fetch er.entry c"
            + " where c.nodeId = ?1 and c.parent.id = ?2 and c.moment > ?3 and c.moment <= ?4"
            + " and r.ownerName = ?5 and r.deletedAt is null")
    Set<Reaction> findByCommentsInRangeAndOwner(UUID nodeId, UUID postingId, long afterMoment, long beforeMoment,
                                               String ownerName);

    @Query("select count(*) from Reaction r where r.entryRevision.entry.id = ?1 and r.moment = ?2")
    int countMoments(UUID entryId, long moment);

    @Query(value = "select r from Reaction r"
            + " left join fetch r.ownerAvatarMediaFile"
            + " where r.entryRevision.entry.id = ?1 and r.negative = ?2"
            + " and r.moment > ?3 and r.moment <= ?4 and r.deletedAt is null",
           countQuery = "select count(*) from Reaction r"
            + " where r.entryRevision.entry.id = ?1 and r.negative = ?2"
            + " and r.moment > ?3 and r.moment <= ?4 and r.deletedAt is null")
    Page<Reaction> findSlice(UUID postingId, boolean negative, long afterMoment, long beforeMoment, Pageable pageable);

    @Query(value = "select r from Reaction r"
            + " left join fetch r.ownerAvatarMediaFile"
            + " where r.entryRevision.entry.id = ?1 and r.negative = ?2 and r.emoji = ?3"
            + " and r.moment > ?4 and r.moment <= ?5 and r.deletedAt is null",
           countQuery = "select count(*) from Reaction r"
            + " where r.entryRevision.entry.id = ?1 and r.negative = ?2 and r.emoji = ?3"
            + " and r.moment > ?4 and r.moment <= ?5 and r.deletedAt is null")
    Page<Reaction> findSliceWithEmoji(UUID postingId, boolean negative, int emoji, long afterMoment, long beforeMoment,
                                      Pageable pageable);

    @Modifying
    @Query("update Reaction r set r.deletedAt = ?2"
            + " where r.entryRevision.id = (select id from EntryRevision er where er.entry.id = ?1)"
            + " and r.deletedAt is null")
    void deleteAllByEntryId(UUID postingId, Timestamp now);

}
