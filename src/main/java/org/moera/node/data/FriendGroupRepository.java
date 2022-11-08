package org.moera.node.data;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendGroupRepository extends JpaRepository<FriendGroup, UUID> {

    @Query("select fg from FriendGroup fg where nodeId = ?1 and id = ?2")
    Optional<FriendGroup> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select fg from FriendGroup fg where nodeId = ?1")
    Collection<FriendGroup> findAllByNodeId(UUID nodeId);

    @Query("select count(*) from FriendGroup fg where nodeId = ?1")
    int countByNodeId(UUID nodeId);

}
