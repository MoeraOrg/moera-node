package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendRepository extends JpaRepository<Friend, UUID> {

    @Query("select fr from Friend fr left join fetch fr.friendGroup fg"
            + " where fg.nodeId = ?1 order by fr.nodeName")
    List<Friend> findAllByNodeId(UUID nodeId);

    @Query("select fr from Friend fr left join fetch fr.friendGroup fg"
            + " where fg.nodeId = ?1 and fg.id = ?2 order by fr.nodeName")
    List<Friend> findAllByNodeIdAndGroup(UUID nodeId, UUID friendGroupId);

    @Query("select fr from Friend fr where fr.friendGroup.nodeId = ?1 and fr.nodeName = ?2")
    List<Friend> findAllByNodeIdAndName(UUID nodeId, String nodeName);

}
