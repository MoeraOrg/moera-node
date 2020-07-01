package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PendingNotificationRepository extends JpaRepository<PendingNotification, UUID> {

    @Query("select p from PendingNotification p order by p.createdAt")
    List<PendingNotification> findAllInOrder();

}
