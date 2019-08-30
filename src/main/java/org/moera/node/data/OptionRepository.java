package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface OptionRepository extends JpaRepository<Option, UUID> {

    List<Option> findAllByNodeId(UUID nodeId);

    Optional<Option> findByNodeIdAndName(UUID nodeId, String name);

    @Modifying
    void deleteByNodeIdAndName(UUID nodeId, String name);

}
