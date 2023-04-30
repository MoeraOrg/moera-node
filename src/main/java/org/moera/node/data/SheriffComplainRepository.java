package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffComplainRepository extends JpaRepository<SheriffComplain, UUID> {

    @Query("select sc from SheriffComplain sc where sc.nodeId = ?1 and sc.id = ?2")
    SheriffComplain findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select sc from SheriffComplain sc"
            + " where sc.nodeId = ?1 and sc.remoteNodeName = ?2 and sc.remoteFeedName = ?3"
            + " and sc.remotePostingId is null and sc.remoteCommentId is null and sc.sheriffDecision is not null")
    List<SheriffComplain> findDecidedByFeed(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                            Pageable pageable);

    @Query("select sc from SheriffComplain sc"
            + " where sc.nodeId = ?1 and sc.remoteNodeName = ?2 and sc.remoteFeedName = ?3"
            + " and sc.remotePostingId = ?4 and sc.remoteCommentId is null and sc.sheriffDecision is not null")
    List<SheriffComplain> findDecidedByPosting(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                               String remotePostingId, Pageable pageable);

    @Query("select sc from SheriffComplain sc"
            + " where sc.nodeId = ?1 and sc.remoteNodeName = ?2 and sc.remoteFeedName = ?3"
            + " and sc.remotePostingId = ?4 and sc.remoteCommentId = ?5 and sc.sheriffDecision is not null")
    List<SheriffComplain> findDecidedByComment(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                               String remotePostingId, String remoteCommentId, Pageable pageable);

}
