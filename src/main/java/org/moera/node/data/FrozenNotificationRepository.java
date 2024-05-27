package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FrozenNotificationRepository extends JpaRepository<FrozenNotification, UUID> {

    @Query("select fn from FrozenNotification fn where fn.nodeId = ?1")
    List<FrozenNotification> findAllByNodeId(UUID nodeId, Pageable pageable);

    @Modifying
    @Query("delete from FrozenNotification fr where fr.deadline < ?1")
    void deleteExpired(Timestamp deadline);

}
