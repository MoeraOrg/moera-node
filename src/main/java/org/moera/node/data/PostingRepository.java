package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query("select count(*) from Posting p where p.moment > ?1 and p.moment <= ?2")
    int countInRange(long beginMoment, long endMoment);

    @Query("select p.moment from Posting p where p.moment > ?1 and p.moment <= ?2")
    Page<Long> findMomentsInRange(long beginMoment, long endMoment, Pageable pageable);

    @Query("select p from Posting p where p.moment > ?1 and p.moment <= ?2 order by p.moment desc")
    List<Posting> findInRange(long beginMoment, long endMoment);

}
