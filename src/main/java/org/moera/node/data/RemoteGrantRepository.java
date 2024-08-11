package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RemoteGrantRepository extends JpaRepository<RemoteGrant, UUID> {

    @Query("select rg from RemoteGrant rg where rg.nodeId = ?1 and rg.remoteNodeName = ?2")
    Optional<RemoteGrant> findByNodeName(UUID nodeId, String nodeName);

}
