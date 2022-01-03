package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteMediaCacheRepository extends JpaRepository<RemoteMediaCache, UUID> {

    @Query("select rmc from RemoteMediaCache rmc"
            + " where (rmc.nodeId = ?1 or rmc.nodeId is null) and rmc.remoteNodeName = ?2 and rmc.remoteMediaId = ?3")
    Collection<RemoteMediaCache> findByMedia(UUID nodeId, String remoteNodeName, String remoteMediaId);

    @Query("delete from RemoteMediaCache rmc where rmc.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
