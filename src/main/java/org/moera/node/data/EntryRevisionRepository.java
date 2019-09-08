package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRevisionRepository extends JpaRepository<EntryRevision, UUID> {

    @Query("select r from EntryRevision r where r.entry.nodeId = ?1 and r.entry.id = ?2"
            + " and r.entry.deletedAt is null and r.id = ?3")
    Optional<EntryRevision> findByEntryIdAndId(UUID nodeId, UUID entryId, UUID id);

    @Query("select r from EntryRevision r where r.entry.nodeId = ?1 and r.entry.id = ?2"
            + " and r.entry.deletedAt is not null and r.id = ?3")
    Optional<EntryRevision> findByDeletedEntryIdAndId(UUID nodeId, UUID entryId, UUID id);

}
