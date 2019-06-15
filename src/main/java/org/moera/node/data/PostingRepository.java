package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query("select count(*) from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3")
    int countInRange(UUID nodeId, long beginMoment, long endMoment);

    @Query("select p.moment from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3")
    Page<Long> findMomentsInRange(UUID nodeId, long beginMoment, long endMoment, Pageable pageable);

    @Query("select p from Posting p where p.nodeId = ?1 and p.moment > ?2 and p.moment <= ?3 order by p.moment desc")
    List<Posting> findInRange(UUID nodeId, long beginMoment, long endMoment);

}
