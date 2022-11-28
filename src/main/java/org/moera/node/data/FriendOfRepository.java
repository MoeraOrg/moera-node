package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FriendOfRepository extends JpaRepository<FriendOf, UUID> {

    @Query("select fo from FriendOf fo where fo.nodeId = ?1 and fo.remoteNodeName = ?2")
    List<FriendOf> findByNodeIdAndRemoteNode(UUID nodeId, String remoteNodeName);

    @Query("select fo from FriendOf fo where fo.nodeId = ?1 and fo.remoteNodeName = ?2 and fo.remoteGroupId = ?3")
    Optional<FriendOf> findByNodeIdAndRemoteGroup(UUID nodeId, String remoteNodeName, String remoteGroupId);

    @Query("update FriendOf fo set fo.remoteAvatarMediaFile = ?3, fo.remoteAvatarShape = ?4"
            + " where fo.nodeId = ?1 and fo.remoteNodeName = ?2")
    @Modifying
    void updateRemoteAvatar(UUID nodeId, String remoteNodeName, MediaFile mediaFile, String shape);

}
