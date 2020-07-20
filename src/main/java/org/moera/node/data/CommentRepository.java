package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("select c from Comment c join fetch c.currentRevision left join fetch c.reactionTotals"
            + " where c.nodeId = ?1 and c.id = ?2 and c.deletedAt is null")
    Optional<Comment> findFullByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Comment c where c.parent.id = ?1 and c.moment = ?2")
    int countMoments(UUID postingId, long moment);

}
