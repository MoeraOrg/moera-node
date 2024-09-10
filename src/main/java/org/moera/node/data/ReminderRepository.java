package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    @Query("select r from Reminder r where r.nodeId = ?1 order by priority asc")
    List<Reminder> findAllByNodeId(UUID nodeId);

}
