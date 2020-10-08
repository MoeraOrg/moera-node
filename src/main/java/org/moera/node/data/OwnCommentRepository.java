package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OwnCommentRepository extends JpaRepository<OwnComment, UUID> {

    @Query("select c from OwnComment c where c.nodeId = ?1 and c.remoteNodeName = ?2 and c.remotePostingId = ?3"
            + " and c.remoteCommentId = ?4")
    Optional<OwnComment> findByRemoteCommentId(UUID nodeId, String remoteNodeName, String remotePostingId,
                                               String remoteCommentId);

    @Modifying
    @Query("delete OwnComment c where c.nodeId = ?1 and c.remoteNodeName = ?2 and c.remotePostingId = ?3"
            + " and c.remoteCommentId = ?4")
    void deleteByRemoteCommentId(UUID nodeId, String remoteNodeName, String remotePostingId, String remoteCommentId);

}
