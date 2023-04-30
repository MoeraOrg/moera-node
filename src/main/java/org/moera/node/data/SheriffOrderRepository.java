package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffOrderRepository extends JpaRepository<SheriffOrder, UUID> {

    @Query("select so from SheriffOrder so where so.nodeId = ?1 and so.id = ?2")
    Optional<SheriffOrder> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select so from SheriffOrder so"
            + " where so.nodeId = ?1 and so.remoteNodeName = ?2 and so.remoteFeedName = ?3"
            + " and so.remotePostingId is null and so.remoteCommentId is null")
    List<SheriffOrder> findByFeed(UUID nodeId, String remoteNodeName, String remoteFeedName, Pageable pageable);

    @Query("select so from SheriffOrder so"
            + " where so.nodeId = ?1 and so.remoteNodeName = ?2 and so.remoteFeedName = ?3"
            + " and so.remotePostingId = ?4 and so.remoteCommentId is null")
    List<SheriffOrder> findByPosting(UUID nodeId, String remoteNodeName, String remoteFeedName, String remotePostingId,
                                     Pageable pageable);

    @Query("select so from SheriffOrder so"
            + " where so.nodeId = ?1 and so.remoteNodeName = ?2 and so.remoteFeedName = ?3"
            + " and so.remotePostingId = ?4 and so.remoteCommentId = ?5")
    List<SheriffOrder> findByComment(UUID nodeId, String remoteNodeName, String remoteFeedName, String remotePostingId,
                                     String remoteCommentId, Pageable pageable);

}
