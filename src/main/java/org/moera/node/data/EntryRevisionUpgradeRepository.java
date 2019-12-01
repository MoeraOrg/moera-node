package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRevisionUpgradeRepository extends JpaRepository<EntryRevisionUpgrade, UUID> {

    @Query("select u from EntryRevisionUpgrade u left join u.entryRevision left join u.entryRevision.entry")
    List<EntryRevisionUpgrade> findPending(Pageable pageable);

}
