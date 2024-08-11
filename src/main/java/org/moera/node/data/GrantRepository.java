package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GrantRepository extends JpaRepository<Grant, UUID> {

    @Query("select g from Grant g where g.nodeId = ?1 and g.nodeName = ?2")
    Optional<Grant> findByNodeName(UUID nodeId, String nodeName);

}
