package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EntryVisitRepository extends JpaRepository<EntryVisit, UUID> {

    @Query(
        "insert into EntryVisit (id, nodeId, entry, clientId, clientName, visitedAt, deadline)"
        + " select ?1, ?2, ?3, ?4, ?5, ?6, ?7"
        + " where not exists ("
        + " select ev from EntryVisit ev"
        + " where ev.nodeId = ?2 and ev.entry = ?3 and ev.deadline > ?6"
        + " and (?4 is null or ev.clientId = ?4)"
        + " and (?5 is null or ev.clientName = ?5)"
        + ")"
    )
    @Modifying
    int insertIfAbsent(
        UUID id, UUID nodeId, Entry entry, String clientId, String clientName, Timestamp visitedAt, Timestamp deadline
    );

    @Query("delete from EntryVisit ev where ev.deadline <= ?1")
    @Modifying
    void deleteExpired(Timestamp now);

}
