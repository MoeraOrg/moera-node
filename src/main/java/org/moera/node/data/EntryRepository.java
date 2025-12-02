package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.sql.Timestamp;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    @Query("select e from Entry e where e.nodeId = ?1 and e.id = ?2 and e.deletedAt is null")
    Optional<Entry> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select e from Entry e left join e.revisions er left join er.attachments a where a.mediaFileOwner.id = ?1")
    Set<Entry> findByMediaId(UUID id);

    @Query("select count(*) from Entry e where e.nodeId = ?1 and e.deletedAt is null and e.ownerName <> ?2")
    int countNotOwnedBy(UUID nodeId, String ownerName);

    @Query(
        "select e from Entry e left join e.parent"
        + " where e.editedAt > e.indexNowUpdatedAt or e.deletedAt is not null AND e.deletedAt > e.indexNowUpdatedAt"
    )
    List<Entry> findByIndexNow(Pageable pageable);

    @Query("update Entry e set e.indexNowUpdatedAt = ?2 where e.id in ?1")
    @Modifying
    void indexedNow(Set<UUID> ids, Timestamp now);

}
