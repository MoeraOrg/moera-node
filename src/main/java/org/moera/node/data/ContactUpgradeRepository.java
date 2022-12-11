package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContactUpgradeRepository extends JpaRepository<ContactUpgrade, UUID> {

    @Query("select count(*) from ContactUpgrade u where u.upgradeType = ?1")
    int countPending(UpgradeType upgradeType);

    @Query("select u from ContactUpgrade u where u.upgradeType = ?1")
    List<ContactUpgrade> findPending(UpgradeType upgradeType, Pageable pageable);

}
