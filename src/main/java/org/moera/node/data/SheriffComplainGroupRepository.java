package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffComplainGroupRepository extends JpaRepository<SheriffComplainGroup, UUID> {

    @Query("select scg from SheriffComplainGroup scg where scg.nodeId = ?1 and scg.id = ?2")
    Optional<SheriffComplainGroup> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select scg from SheriffComplainGroup scg"
            + " where scg.nodeId = ?1 and scg.remoteNodeName = ?2 and scg.remoteFeedName = ?3"
            + " and scg.remotePostingId is null and scg.remoteCommentId is null")
    Optional<SheriffComplainGroup> findByFeed(UUID nodeId, String remoteNodeName, String remoteFeedName);

    @Query("select scg from SheriffComplainGroup scg"
            + " where scg.nodeId = ?1 and scg.remoteNodeName = ?2 and scg.remoteFeedName = ?3"
            + " and scg.remotePostingId = ?4 and scg.remoteCommentId is null")
    Optional<SheriffComplainGroup> findByPosting(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                                 String remotePostingId);

    @Query("select scg from SheriffComplainGroup scg"
            + " where scg.nodeId = ?1 and scg.remoteNodeName = ?2 and scg.remoteFeedName = ?3"
            + " and scg.remotePostingId = ?4 and scg.remoteCommentId = ?5")
    Optional<SheriffComplainGroup> findByComment(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                                 String remotePostingId, String remoteCommentId);

    @Query("select count(*) from SheriffComplainGroup scg where scg.nodeId = ?1 and scg.moment = ?2")
    int countMoments(UUID nodeId, long moment);

}
