package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.ownerName = ?2 and r.deletedAt is null")
    Reaction findByEntryAndOwner(UUID postingId, String ownerName);

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.ownerName = ?2"
            + " and r.deletedAt is not null")
    List<Reaction> findDeletedByEntryAndOwner(UUID postingId, String ownerName, Pageable pageable);

    @Query("select r from Reaction r left join r.entryRevision where r.deadline < ?1")
    List<Reaction> findExpired(Timestamp deadline);

}
