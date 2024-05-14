package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PendingJobRepository extends JpaRepository<PendingJob, UUID> {

    @Query("select pj from PendingJob pj where pj.waitUntil is null or pj.waitUntil < ?1")
    Collection<PendingJob> findAllBefore(Timestamp timestamp);

    @Query("select count(*) from PendingJob pj where jobType = ?1")
    int countByType(String jobType);

}
