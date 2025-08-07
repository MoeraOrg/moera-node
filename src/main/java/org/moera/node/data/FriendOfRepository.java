package org.moera.node.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendOfRepository extends JpaRepository<FriendOf, UUID> {

    @Query(
        "select fo from FriendOf fo left join fetch fo.contact c left join fetch c.remoteAvatarMediaFile"
        + " where fo.nodeId = ?1"
    )
    List<FriendOf> findAllByNodeId(UUID nodeId);

    @Query("select count(distinct fo.remoteNodeName) from FriendOf fo where fo.nodeId = ?1")
    int countByNodeId(UUID nodeId);

    @Query(
        "select fo from FriendOf fo left join fetch fo.contact c left join fetch c.remoteAvatarMediaFile"
        + " where fo.nodeId = ?1 and fo.remoteNodeName = ?2"
    )
    List<FriendOf> findByNodeIdAndRemoteNode(UUID nodeId, String remoteNodeName);

    @Query("select fo from FriendOf fo where fo.nodeId = ?1 and fo.remoteNodeName in ?2 order by fo.remoteNodeName")
    List<FriendOf> findByRemoteNodes(UUID nodeId, Collection<String> remoteNodeNames);

    @Query(
        "select fo from FriendOf fo left join fetch fo.contact c left join fetch c.remoteAvatarMediaFile"
        + " where fo.nodeId = ?1 and fo.remoteNodeName = ?2 and fo.remoteGroupId = ?3"
    )
    Optional<FriendOf> findByNodeIdAndRemoteGroup(UUID nodeId, String remoteNodeName, String remoteGroupId);

}
