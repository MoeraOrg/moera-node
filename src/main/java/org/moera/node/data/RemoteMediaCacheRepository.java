package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteMediaCacheRepository extends JpaRepository<RemoteMediaCache, UUID> {

    @Query("select rmc from RemoteMediaCache rmc where rmc.remoteNodeName = ?1 and rmc.remoteMediaId = ?2")
    Optional<RemoteMediaCache> findByMedia(String remoteNodeName, String remoteMediaId);

    @Query("delete from RemoteMediaCache rmc where rmc.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
