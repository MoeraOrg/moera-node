package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffComplainRepository extends JpaRepository<SheriffComplain, UUID> {

    @Query("select sc from SheriffComplain sc where sc.nodeId = ?1 and sc.id = ?2")
    Optional<SheriffComplain> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select sc from SheriffComplain sc where sc.nodeId = ?1 and sc.group.id = ?2 order by sc.createdAt")
    List<SheriffComplain> findByGroupId(UUID nodeId, UUID groupId);

}
