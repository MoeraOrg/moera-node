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

}
