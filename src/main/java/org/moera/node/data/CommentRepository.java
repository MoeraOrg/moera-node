package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface CommentRepository extends JpaRepository<Comment, UUID>, QuerydslPredicateExecutor<Comment> {

    @Query("select c from Comment c where c.nodeId = ?1 and c.id = ?2 and c.deletedAt is null")
    Optional<Comment> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select c from Comment c"
            + " join fetch c.currentRevision cr left join fetch c.ownerAvatarMediaFile"
            + " left join fetch c.reactionTotals left join fetch c.parent"
            + " where c.nodeId = ?1 and c.id = ?2 and c.deletedAt is null")
    Optional<Comment> findFullByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Comment c where c.parent.id = ?1 and c.moment = ?2")
    int countMoments(UUID postingId, long moment);

    @Query("select c from Comment c"
            + " left join fetch c.currentRevision cr left join fetch c.ownerAvatarMediaFile"
            + " left join fetch c.reactionTotals"
            + " where c.nodeId = ?1 and c.parent.id = ?2 and c.moment > ?3 and c.moment <= ?4 and c.deletedAt is null")
    Set<Comment> findInRange(UUID nodeId, UUID parentId, long afterMoment, long beforeMoment);

    @Query(value = "select c from Comment c join fetch c.currentRevision"
            + " where c.nodeId = ?1 and c.parent.id = ?2 and c.deletedAt is null",
           countQuery = "select count(c) from Comment c"
            + " where c.nodeId = ?1 and c.parent.id = ?2 and c.deletedAt is null")
    Page<Comment> findByNodeIdAndParentId(UUID nodeId, UUID parentId, Pageable pageable);

    @Query("select count(*) from Comment c where c.nodeId = ?1 and c.parent.id = ?2"
            + " and c.moment > ?3 and c.moment <= ?4")
    int countInRange(UUID nodeId, UUID parentId, long afterMoment, long beforeMoment);

    @Query("select c.moment from Comment c where c.nodeId = ?1 and c.parent.id = ?2"
            + " and c.moment > ?3 and c.moment <= ?4")
    Page<Long> findMomentsInRange(UUID nodeId, UUID parentId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select c from Comment c left join c.parent p left join p.stories s"
            + " where c.nodeId = ?1 and c.ownerName = ?2 and c.deletedAt is null and s.feedName = ?3")
    List<Comment> findByOwnerNameAndFeed(UUID nodeId, String ownerName, String feedName);

    @Query("select count(*) from Comment c left join c.parent p left join p.stories s"
            + " where c.nodeId = ?1 and c.ownerName = ?2 and c.deletedAt is null and s.feedName = ?3")
    int countByOwnerNameAndFeed(UUID nodeId, String ownerName, String feedName);

    @Query("update Comment c set c.sheriffUserListReferred = ?4"
            + " where c.nodeId = ?1 and c.ownerName = ?2 and c.deletedAt is null"
            + " and exists(select s from Story s where s.entry = c.parent and s.feedName = ?3)")
    @Modifying
    void updateSheriffReferredByOwnerNameAndFeed(UUID nodeId, String ownerName, String feedName, boolean referred);

    @Query("select c from Comment c left join fetch c.currentRevision left join fetch c.parent"
            + " where c.deletedAt is null and c.currentRevision.deadline < ?1")
    List<Comment> findExpiredUnsigned(Timestamp deadline);

    @Query("select c from Comment c where c.deletedAt is not null and c.deadline < ?1")
    List<Comment> findExpired(Timestamp deadline);

}
