package org.moera.node.data;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendRepository extends JpaRepository<Friend, UUID> {

    @Query(
        "select fr from Friend fr left join fetch fr.contact c left join fetch c.remoteAvatarMediaFile"
        + " left join fetch fr.friendGroup fg"
        + " where fr.nodeId = ?1 order by fr.remoteNodeName"
    )
    List<Friend> findAllByNodeId(UUID nodeId);

    @Query(
        "select new org.moera.node.data.FriendGroupTotal(fr.friendGroup.id, count(*)) from Friend fr"
        + " where fr.nodeId = ?1 group by fr.friendGroup.id"
    )
    List<FriendGroupTotal> countGroupsByNodeId(UUID nodeId);

    @Query(
        "select fr from Friend fr left join fetch fr.contact c left join fetch c.remoteAvatarMediaFile"
        + " left join fetch fr.friendGroup fg"
        + " where fg.nodeId = ?1 and fg.id = ?2 order by fr.remoteNodeName"
    )
    List<Friend> findAllByNodeIdAndGroup(UUID nodeId, UUID friendGroupId);

    @Query(
        "select fr from Friend fr left join fetch fr.contact c left join fetch c.remoteAvatarMediaFile"
        + " left join fetch fr.friendGroup fg"
        + " where fr.nodeId = ?1 and fr.remoteNodeName = ?2"
    )
    List<Friend> findAllByNodeIdAndName(UUID nodeId, String remoteNodeName);

    @Query("select fr from Friend fr where fr.nodeId = ?1")
    List<Friend> findByNodeId(UUID nodeId);

    @Query("select fr from Friend fr where fr.nodeId = ?1 and fr.remoteNodeName = ?2")
    List<Friend> findByNodeIdAndName(UUID nodeId, String remoteNodeName);

    @Query("select fr from Friend fr where fr.nodeId = ?1 and fr.remoteNodeName in ?2 order by fr.remoteNodeName")
    List<Friend> findByNames(UUID nodeId, Collection<String> remoteNodeNames);

}
