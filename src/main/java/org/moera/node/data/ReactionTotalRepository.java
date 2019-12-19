package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReactionTotalRepository extends JpaRepository<ReactionTotal, UUID> {

    @Query("select rt from ReactionTotal rt where rt.entryRevision.id = ?1 and rt.negative = ?2 and rt.emoji = ?3")
    ReactionTotal findByEntryRevision(UUID entryRevisionId, boolean negative, int emoji);

    @Query("select rt from ReactionTotal rt where rt.entry.id = ?1 and rt.negative = ?2 and rt.emoji = ?3")
    ReactionTotal findByEntry(UUID entryId, boolean negative, int emoji);

}
