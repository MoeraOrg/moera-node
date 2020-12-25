package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, UUID> {

    @Query("select s from WebPushSubscription s where s.nodeId = ?1 and s.publicKey = ?2 and s.authKey = ?3")
    Optional<WebPushSubscription> findByKeys(UUID nodeId, String publicKey, String authKey);

}
