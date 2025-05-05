package org.moera.node.data;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserListItemRepository extends JpaRepository<UserListItem, UUID> {

    @Query("select i from UserListItem i where i.nodeId = ?1 and i.listName = ?2 and i.nodeName = ?3")
    Optional<UserListItem> findByListAndNodeName(UUID nodeId, String listName, String nodeName);

    @Query("select i from UserListItem i where i.nodeId = ?1 and i.listName = ?2 and i.nodeName in (?3)")
    Collection<UserListItem> findByListAndNodeNames(UUID nodeId, String listName, Collection<String> nodeNames);

    @Query("select count(*) from UserListItem i where i.nodeId = ?1 and i.listName = ?2")
    int countByList(UUID nodeId, String listName);

    @Query(
        "select i from UserListItem i"
        + " where i.nodeId = ?1 and i.listName = ?2 and i.moment > ?3 and i.moment <= ?4"
    )
    Page<UserListItem> findInRange(
        UUID nodeId, String listName, long afterMoment, long beforeMoment, Pageable pageable
    );

    @Query(
        "select count(*) from UserListItem i"
        + " where i.nodeId = ?1 and i.listName = ?2 and i.moment > ?3 and i.moment <= ?4"
    )
    int countInRange(UUID nodeId, String listName, long afterMoment, long beforeMoment);

    @Query("select count(*) from UserListItem i where i.nodeId = ?1 and i.moment = ?2")
    int countMoments(UUID nodeId, long moment);

}
