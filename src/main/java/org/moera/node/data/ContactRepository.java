package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ContactRepository extends JpaRepository<Contact, UUID>, QuerydslPredicateExecutor<Contact> {

    @Query("select c from Contact c where c.nodeId = ?1 and c.remoteNodeName = ?2")
    Optional<Contact> findByRemoteNode(UUID nodeId, String remoteNodeName);

    @Modifying
    @Query("delete from Contact c where c.nodeId = ?1 and c.remoteNodeName = ?2")
    void deleteByRemoteNode(UUID nodeId, String remoteNodeName);

    @Query("select c from Contact c where c.updatedAt < ?1")
    Collection<Contact> findAllUpdatedBefore(Timestamp deadline);

}
