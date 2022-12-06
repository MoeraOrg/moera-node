package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;

import org.moera.node.model.AskSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AskHistoryRepository extends JpaRepository<AskHistory, UUID> {

    @Query("select count(*) from AskHistory ah where ah.nodeId = ?1 and ah.remoteNodeName = ?2")
    int countByRemoteNode(UUID nodeId, String remoteNodeName);

    @Query("select max(ah.createdAt) from AskHistory ah"
            + " where ah.nodeId = ?1 and ah.remoteNodeName = ?2 and ah.subject = ?3")
    Timestamp findLastCreatedAt(UUID nodeId, String remoteNodeName, AskSubject subject);

}
