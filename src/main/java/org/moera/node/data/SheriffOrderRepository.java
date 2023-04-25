package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffOrderRepository extends JpaRepository<SheriffOrder, UUID> {

    @Query("select so from SheriffOrder so where so.nodeId = ?1 and so.id = ?2")
    Optional<SheriffOrder> findByNodeIdAndId(UUID nodeId, UUID id);

}
