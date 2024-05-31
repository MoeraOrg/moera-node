package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SheriffComplaintGroupRepository extends JpaRepository<SheriffComplaintGroup, UUID> {

    @Query("select scg from SheriffComplaintGroup scg where scg.nodeId = ?1 and scg.id = ?2")
    Optional<SheriffComplaintGroup> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from SheriffComplaintGroup scg where scg.nodeId = ?1")
    int countByNodeId(UUID nodeId);

    @Query("select count(*) from SheriffComplaintGroup scg where scg.nodeId = ?1 and scg.status = ?2")
    int countByStatus(UUID nodeId, SheriffComplaintStatus status);

    @Query("select scg from SheriffComplaintGroup scg"
            + " where scg.nodeId = ?1 and scg.remoteNodeName = ?2 and scg.remoteFeedName = ?3"
            + " and scg.remotePostingId is null and scg.remoteCommentId is null")
    Optional<SheriffComplaintGroup> findByFeed(UUID nodeId, String remoteNodeName, String remoteFeedName);

    @Query("select scg from SheriffComplaintGroup scg"
            + " where scg.nodeId = ?1 and scg.remoteNodeName = ?2 and scg.remoteFeedName = ?3"
            + " and scg.remotePostingId = ?4 and scg.remoteCommentId is null")
    Optional<SheriffComplaintGroup> findByPosting(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                                  String remotePostingId);

    @Query("select scg from SheriffComplaintGroup scg"
            + " where scg.nodeId = ?1 and scg.remoteNodeName = ?2 and scg.remoteFeedName = ?3"
            + " and scg.remotePostingId = ?4 and scg.remoteCommentId = ?5")
    Optional<SheriffComplaintGroup> findByComment(UUID nodeId, String remoteNodeName, String remoteFeedName,
                                                  String remotePostingId, String remoteCommentId);

    @Query("select count(*) from SheriffComplaintGroup scg where scg.nodeId = ?1 and scg.moment = ?2")
    int countMoments(UUID nodeId, long moment);

    @Query("select scg from SheriffComplaintGroup scg where scg.nodeId = ?1 and scg.moment > ?2 and scg.moment <= ?3")
    Page<SheriffComplaintGroup> findInRange(UUID nodeId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select scg from SheriffComplaintGroup scg"
            + " where scg.nodeId = ?1 and scg.moment > ?2 and scg.moment <= ?3 and scg.status = ?4")
    Page<SheriffComplaintGroup> findByStatusInRange(UUID nodeId, long afterMoment, long beforeMoment,
                                                    SheriffComplaintStatus status, Pageable pageable);

    @Query("select count(*) from SheriffComplaintGroup scg where scg.nodeId = ?1"
            + " and scg.moment > ?2 and scg.moment <= ?3")
    int countInRange(UUID nodeId, long afterMoment, long beforeMoment);

    @Query("select count(*) from SheriffComplaintGroup scg where scg.nodeId = ?1"
            + " and scg.moment > ?2 and scg.moment <= ?3 and scg.status = ?4")
    int countByStatusInRange(UUID nodeId, long afterMoment, long beforeMoment, SheriffComplaintStatus status);

}
