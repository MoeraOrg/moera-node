package org.moera.node.data;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    @Query("select e from Entry e left join e.revisions er left join er.attachments a where a.mediaFileOwner.id = ?1")
    Set<Entry> findByMediaId(UUID id);

}
