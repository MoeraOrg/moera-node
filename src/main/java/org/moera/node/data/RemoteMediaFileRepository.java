package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteMediaFileRepository extends JpaRepository<RemoteMediaFile, UUID> {

    @Query(
        "select distinct"
        + " new org.moera.node.data.RemoteMediaLeaseKey(rmf.nodeId, rmf.nodeName, rmf.leaseId)"
        + " from RemoteMediaFile rmf"
        + " where rmf.leaseId is not null and rmf.deadline is not null and rmf.deadline < ?1"
    )
    Set<RemoteMediaLeaseKey> findUnusedLeaseKeys(Timestamp deadline);

    @Query("select count(*) from RemoteMediaFile rmf where rmf.nodeId = ?1 and rmf.nodeName = ?2 and rmf.leaseId = ?2")
    int countUsedByNodeIdAndLeaseId(UUID nodeId, String remoteNodeName, String leaseId);

    @Query("delete from RemoteMediaFile rmf where rmf.deadline is not null and rmf.deadline < ?1")
    @Modifying
    void deleteUnused(Timestamp deadline);

}
