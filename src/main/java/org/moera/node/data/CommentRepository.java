package org.moera.node.data;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("select c from Comment c join fetch c.currentRevision left join fetch c.reactionTotals"
            + " where c.nodeId = ?1 and c.id = ?2 and c.deletedAt is null")
    Optional<Comment> findFullByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Comment c where c.parent.id = ?1 and c.moment = ?2")
    int countMoments(UUID postingId, long moment);

    @Query("select c from Comment c left join fetch c.currentRevision"
            + " where c.nodeId = ?1 and c.parent.id = ?2 and c.moment > ?3 and c.moment <= ?4")
    Set<Comment> findInRange(UUID nodeId, UUID parentId, long afterMoment, long beforeMoment);

    @Query("select c from Comment c where c.nodeId = ?1 and c.parent.id = ?2 and c.moment > ?3 and c.moment <= ?4")
    Page<Comment> findSlice(UUID nodeId, UUID parentId, long afterMoment, long beforeMoment, Pageable pageable);

}
