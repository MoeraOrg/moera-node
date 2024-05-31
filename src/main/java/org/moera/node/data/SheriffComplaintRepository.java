package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffComplaintRepository extends JpaRepository<SheriffComplaint, UUID> {

    @Query("select sc from SheriffComplaint sc where sc.nodeId = ?1 and sc.id = ?2")
    Optional<SheriffComplaint> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select sc from SheriffComplaint sc where sc.nodeId = ?1 and sc.group.id = ?2 order by sc.createdAt")
    List<SheriffComplaint> findByGroupId(UUID nodeId, UUID groupId);

}
