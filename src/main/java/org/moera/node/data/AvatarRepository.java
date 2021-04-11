package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AvatarRepository extends JpaRepository<Avatar, UUID> {

    @Query("select a from Avatar a where a.nodeId = ?1 and a.id = ?2")
    Optional<Avatar> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select a from Avatar a where a.nodeId = ?1 and a.current = true")
    Optional<Avatar> findByNodeIdAndCurrent(UUID nodeId);

    @Query("select a from Avatar a where a.nodeId = ?1 order by a.createdAt desc")
    List<Avatar> findAllByNodeId(UUID nodeId);

    @Query("update Avatar a set a.current = false where a.nodeId = ?1")
    @Modifying
    void resetCurrent(UUID nodeId);

}
