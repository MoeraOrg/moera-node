package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PublicPageRepository extends JpaRepository<PublicPage, Long> {

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id is null and p.afterMoment = ?2")
    PublicPage findByAfterMoment(UUID nodeId, long afterMoment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id = ?2 and p.afterMoment = ?3")
    PublicPage findByAfterMomentForEntry(UUID nodeId, UUID entryId, long afterMoment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id is null and p.beforeMoment = ?2")
    PublicPage findByBeforeMoment(UUID nodeId, long beforeMoment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id = ?2 and p.beforeMoment = ?3")
    PublicPage findByBeforeMomentForEntry(UUID nodeId, UUID entryId, long beforeMoment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id is null"
            + " and ?2 > p.afterMoment and ?2 <= p.beforeMoment")
    PublicPage findContaining(UUID nodeId, long moment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id = ?2"
            + " and ?3 > p.afterMoment and ?3 <= p.beforeMoment")
    PublicPage findContainingForEntry(UUID nodeId, UUID entryId, long moment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id is null and p.beforeMoment <= ?2")
    Page<PublicPage> findAllBeforeMoment(UUID nodeId, long moment, Pageable pageable);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.entry.id = ?2 and p.beforeMoment >= ?3")
    Page<PublicPage> findAllAfterMomentForEntry(UUID nodeId, UUID entryId, long moment, Pageable pageable);

    // Note: page numbers are counted from 1
    @Query("select count(*) from PublicPage p where p.nodeId = ?1 and p.entry.id is null and p.beforeMoment >= ?2")
    int countNumber(UUID nodeId, long moment);

    @Query("select count(*) from PublicPage p where p.nodeId = ?1 and p.entry.id = ?2 and p.beforeMoment <= ?3")
    int countNumberForEntry(UUID nodeId, UUID entryId, long moment);

    @Query("select count(*) from PublicPage p where p.nodeId = ?1 and p.entry.id is null")
    int countTotal(UUID nodeId);

    @Query("select count(*) from PublicPage p where p.nodeId = ?1 and p.entry.id = ?2")
    int countTotalForEntry(UUID nodeId, UUID entryId);

}
