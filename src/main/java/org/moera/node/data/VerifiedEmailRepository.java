package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VerifiedEmailRepository extends JpaRepository<VerifiedEmail, UUID> {

    @Query("select count(ve) from VerifiedEmail ve where ve.nodeId = ?1 and ve.email = ?2")
    int countByNodeIdAndEmail(UUID nodeId, String email);

}
