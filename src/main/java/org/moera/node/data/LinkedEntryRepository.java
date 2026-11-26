package org.moera.node.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkedEntryRepository extends JpaRepository<LinkedEntry, UUID> {

    @Query("select le from LinkedEntry le where le.entryRevision.entry.nodeId = ?1 and le.id = ?2")
    Optional<LinkedEntry> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select le from LinkedEntry le where le.entryRevision.entry.nodeId = ?1 and le.entryRevision.id = ?2")
    List<LinkedEntry> findByRevisionId(UUID nodeId, UUID revisionId);

    @Query(
        "select le from LinkedEntry le"
        + " where le.entryRevision.entry.nodeId = ?1 and le.entryRevision.id in ?2 and le.linkedEntry is not null"
    )
    List<LinkedEntry> findResolvedByRevisionIds(UUID nodeId, Collection<UUID> revisionIds);

    @Query("select count(le) from LinkedEntry le where le.linkedEntry.id = ?1")
    long countByLinkedEntryId(UUID entryId);

    @Query("""
        select le from LinkedEntry le
        join fetch le.entryRevision revision
        join fetch revision.entry source
        join fetch source.currentRevision current
        where le.linkedEntry.id = ?1 and le.linkedEntry.receiverName is null
            and source.deletedAt is null and source.receiverName is null
            and le.sharingMoment is not null and revision.signature is not null
            and (revision.id = current.id or current.signature is null)
        order by le.sharingMoment desc
        """)
    List<LinkedEntry> findSharingLinks(UUID postingId);

    @Query(
        value = "select pg_advisory_xact_lock(hashtextextended('posting-own-sharing:' || ?1, 0))",
        nativeQuery = true
    )
    void lockSharingMoments(String postingId);

    @Query("select coalesce(max(le.sharingMoment), 0) from LinkedEntry le where le.linkedEntry.id = ?1")
    long lastSharingMoment(UUID postingId);

}
