package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteConnectivityRepository extends JpaRepository<RemoteConnectivity, UUID> {

    @Query("select rc from RemoteConnectivity rc where rc.remoteNodeName = ?1")
    Optional<RemoteConnectivity> findByRemoteNodeName(String remoteNodeName);

    @Modifying
    @Query("delete from RemoteConnectivity rc where rc.remoteNodeName = ?1")
    void deleteByRemoteNodeName(String remoteNodeName);

}
