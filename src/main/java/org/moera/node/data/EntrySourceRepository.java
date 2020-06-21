package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntrySourceRepository extends JpaRepository<EntrySource, UUID> {

    @Query("select sr from EntrySource sr where sr.entry.id = ?1")
    List<EntrySource> findAllByEntryId(UUID entryId);

}
