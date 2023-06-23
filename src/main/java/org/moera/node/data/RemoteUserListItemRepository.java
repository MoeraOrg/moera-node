package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteUserListItemRepository extends JpaRepository<RemoteUserListItem, UUID> {

    @Query("select i from RemoteUserListItem i"
            + " where i.nodeId = ?1 and i.listNodeName = ?2 and i.listName = ?3 and i.absent = false")
    Page<RemoteUserListItem> findNotAbsentByList(UUID nodeId, String listNodeName, String listName, Pageable pageable);

    @Query("select i from RemoteUserListItem i"
            + " where i.nodeId = ?1 and i.listNodeName = ?2 and i.listName = ?3 and i.nodeName = ?4")
    Optional<RemoteUserListItem> findByListAndNodeName(
            UUID nodeId, String listNodeName, String listName, String nodeName);

    @Query("select i from RemoteUserListItem i"
            + " where i.nodeId = ?1 and i.listNodeName = ?2 and i.listName = ?3 and i.nodeName in (?4)")
    Collection<RemoteUserListItem> findByListAndNodeNames(
            UUID nodeId, String listNodeName, String listName, Collection<String> nodeNames);

    @Query("select i from RemoteUserListItem i"
            + " where i.nodeId = ?1 and i.listName = ?2 and i.nodeName = ?3 and i.absent = false")
    Collection<RemoteUserListItem> findByNodeNameNotAbsent(UUID nodeId, String listName, String nodeName);

    @Query("delete from RemoteUserListItem i where i.absent = true and i.deadline < ?1")
    @Modifying
    void deleteExpiredAbsent(Timestamp deadline);

    @Query("select i from RemoteUserListItem i where i.absent = false and i.deadline < ?1")
    Collection<RemoteUserListItem> findExpiredNotAbsent(Timestamp deadline);

}
