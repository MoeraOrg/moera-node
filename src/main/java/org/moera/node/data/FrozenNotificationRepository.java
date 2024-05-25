package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FrozenNotificationRepository extends JpaRepository<FrozenNotification, UUID> {

    @Modifying
    @Query("delete from FrozenNotification fr where fr.deadline < ?1")
    void deleteExpired(Timestamp deadline);

}
