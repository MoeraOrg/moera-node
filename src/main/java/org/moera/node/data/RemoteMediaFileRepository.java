package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteMediaFileRepository extends JpaRepository<RemoteMediaFile, UUID> {

    @Query("delete from RemoteMediaFile rmf where rmf.deadline is not null and rmf.deadline < ?1")
    @Modifying
    void deleteUnused(Timestamp deadline);

}
