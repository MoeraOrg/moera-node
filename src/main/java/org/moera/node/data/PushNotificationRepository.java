package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PushNotificationRepository extends JpaRepository<PushNotification, UUID> {

    @Query("select max(pn.moment) from PushNotification pn where pn.pushClient.id = ?1")
    Long findLastMoment(UUID pushClientId);

    @Query("select pn from PushNotification pn where pn.pushClient.id = ?1 and pn.moment > ?2")
    Page<PushNotification> findSlice(UUID pushClientId, long afterMoment, Pageable pageable);

    @Query("delete from PushNotification pn where pn.pushClient.id = ?1 and pn.moment <= ?2")
    @Modifying
    void deleteTill(UUID pushClientId, long moment);

    @Query("delete from PushNotification pn where pn.pushClient.nodeId = ?1 and pn.moment <= ?2")
    @Modifying
    void deleteAllTill(UUID nodeId, long moment);

}
