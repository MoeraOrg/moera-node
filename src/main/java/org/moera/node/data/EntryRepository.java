package org.moera.node.data;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    @Query("select e from Entry e where e.nodeId = ?1 and e.id = ?2 and e.deletedAt is null")
    Optional<Entry> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select e from Entry e left join e.revisions er left join er.attachments a where a.mediaFileOwner.id = ?1")
    Set<Entry> findByMediaId(UUID id);

    @Query("select count(*) from Entry e where e.nodeId = ?1 and e.deletedAt is null and e.ownerName <> ?2")
    int countNotOwnedBy(UUID nodeId, String ownerName);

}
