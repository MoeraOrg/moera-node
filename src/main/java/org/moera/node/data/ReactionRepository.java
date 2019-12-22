package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    @Query("select r from Reaction r where r.entryRevision.entry.id = ?1 and r.ownerName = ?2 and r.deletedAt is null")
    Reaction findByPostingAndOwner(UUID postingId, String ownerName);

}
