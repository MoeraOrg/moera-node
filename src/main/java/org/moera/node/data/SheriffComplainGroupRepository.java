package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffComplainGroupRepository extends JpaRepository<SheriffComplainGroup, UUID> {

    @Query("select scg from SheriffComplainGroup scg where scg.nodeId = ?1 and scg.id = ?2")
    Optional<SheriffComplainGroup> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from SheriffComplainGroup scg where scg.nodeId = ?1")
    int countByNodeId(UUID nodeId);

    @Query("select count(*) from SheriffComplainGroup scg where scg.nodeId = ?1 and scg.status = ?2")
    int countByStatus(UUID nodeId, SheriffComplainStatus status);

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

    @Query("select scg from SheriffComplainGroup scg where scg.nodeId = ?1 and scg.moment > ?2 and scg.moment <= ?3")
    Page<SheriffComplainGroup> findInRange(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select scg from SheriffComplainGroup scg"
            + " where scg.nodeId = ?1 and scg.moment > ?2 and scg.moment <= ?3 and scg.status = ?4")
    Page<SheriffComplainGroup> findByStatusInRange(UUID nodeId, long afterMoment, long beforeMoment,
                                                   SheriffComplainStatus status, Pageable pageable);

    @Query("select count(*) from SheriffComplainGroup scg where scg.nodeId = ?1"
            + " and scg.moment > ?2 and scg.moment <= ?3")
    int countInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select count(*) from SheriffComplainGroup scg where scg.nodeId = ?1"
            + " and scg.moment > ?2 and scg.moment <= ?3 and scg.status = ?4")
    int countByStatusInRange(UUID nodeId, long afterMoment, long beforeMoment, SheriffComplainStatus status);

}
