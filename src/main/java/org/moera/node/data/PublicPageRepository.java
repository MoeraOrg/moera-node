package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PublicPageRepository extends JpaRepository<PublicPage, Long> {

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.beginMoment = ?2")
    PublicPage findByBeginMoment(UUID nodeId, long beginMoment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.endMoment = ?2")
    PublicPage findByEndMoment(UUID nodeId, long endMoment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and ?2 > p.beginMoment and ?2 <= p.endMoment")
    PublicPage findContaining(UUID nodeId, long moment);

    @Query("select p from PublicPage p where p.nodeId = ?1 and p.endMoment <= ?2")
    Page<PublicPage> findAllBeforeEndMoment(UUID nodeId, long endMoment, Pageable pageable);

    // Note: page numbers are counted from 1
    @Query("select count(*) from PublicPage p where p.nodeId = ?1 and p.endMoment >= ?2")
    int countNumber(UUID nodeId, long moment);

    @Query("select count(*) from PublicPage p where p.nodeId = ?1")
    int countTotal(UUID nodeId);

}
