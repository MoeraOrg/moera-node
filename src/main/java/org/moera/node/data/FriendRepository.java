package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FriendRepository extends JpaRepository<Friend, UUID> {

    @Query("select fr from Friend fr left join fetch fr.friendGroup fg where fr.nodeId = ?1 order by fr.remoteNodeName")
    List<Friend> findAllByNodeId(UUID nodeId);

    @Query("select fr from Friend fr left join fetch fr.friendGroup fg"
            + " where fg.nodeId = ?1 and fg.id = ?2 order by fr.remoteNodeName")
    List<Friend> findAllByNodeIdAndGroup(UUID nodeId, UUID friendGroupId);

    @Query("select fr from Friend fr left join fetch fr.friendGroup fg where fr.nodeId = ?1 and fr.remoteNodeName = ?2")
    List<Friend> findAllByNodeIdAndName(UUID nodeId, String remoteNodeName);

    @Query("update Friend fr set fr.remoteFullName = ?3, fr.remoteGender = ?4"
            + " where fr.nodeId = ?1 and fr.remoteNodeName = ?2")
    @Modifying
    void updateRemoteFullNameAndGender(UUID nodeId, String remoteNodeName, String remoteFullName, String remoteGender);

    @Query("update Friend fr set fr.remoteAvatarMediaFile = ?3, fr.remoteAvatarShape = ?4"
            + " where fr.nodeId = ?1 and fr.remoteNodeName = ?2")
    @Modifying
    void updateRemoteAvatar(UUID nodeId, String remoteNodeName, MediaFile mediaFile, String shape);

}
