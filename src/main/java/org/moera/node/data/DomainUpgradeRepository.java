package org.moera.node.data;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DomainUpgradeRepository extends JpaRepository<DomainUpgrade, UUID> {

    @Query("select u from DomainUpgrade u where u.upgradeType = ?1")
    Set<DomainUpgrade> findPending(UpgradeType upgradeType);

    @Query("delete from DomainUpgrade u where u.upgradeType = ?1 and u.nodeId = ?2")
    @Modifying
    void deleteByTypeAndNode(UpgradeType upgradeType, UUID nodeId);

}
