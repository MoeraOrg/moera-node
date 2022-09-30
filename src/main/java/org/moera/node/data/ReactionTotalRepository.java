package org.moera.node.data;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReactionTotalRepository extends JpaRepository<ReactionTotal, UUID> {

    @Query("select rt from ReactionTotal rt where rt.entryRevision.id = ?1 and rt.negative = ?2 and rt.emoji = ?3")
    ReactionTotal findByEntryRevisionId(UUID entryRevisionId, boolean negative, int emoji);

    @Query("select rt from ReactionTotal rt where rt.entry.id = ?1 and rt.negative = ?2 and rt.emoji = ?3")
    ReactionTotal findByEntryId(UUID entryId, boolean negative, int emoji);

    @Query("select rt from ReactionTotal rt where rt.entry.id = ?1 and rt.emoji is null")
    ReactionTotal findSummaryByEntryId(UUID entryId);

    @Query("select rt from ReactionTotal rt where rt.entry.id = ?1 and rt.total != 0")
    Set<ReactionTotal> findAllByEntryId(UUID entryId);

    @Query("select rt from ReactionTotal rt where rt.entry.id in (?1) and rt.total != 0")
    Set<ReactionTotal> findAllByEntryIds(Collection<UUID> entryIds);

    @Modifying
    @Query("delete from ReactionTotal rt where rt.entry.id = ?1"
            + " or rt.entryRevision.id = any(select id from EntryRevision er where er.entry.id = ?1)")
    void deleteAllByEntryId(UUID postingId);

}
