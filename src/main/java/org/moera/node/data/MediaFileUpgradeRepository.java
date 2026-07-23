package org.moera.node.data;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileUpgradeRepository extends JpaRepository<MediaFileUpgrade, Long> {

    @Query(
        "select u from MediaFileUpgrade u join fetch u.mediaFile"
        + " where u.upgradeType = ?1 order by u.id"
    )
    List<MediaFileUpgrade> findPending(UpgradeType upgradeType, Pageable pageable);

    @Query("delete from MediaFileUpgrade u where u.id = ?1")
    @Modifying
    void deleteById(long id);

}
