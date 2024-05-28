package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.moera.node.auth.principal.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface StoryRepository extends JpaRepository<Story, UUID>, QuerydslPredicateExecutor<Story> {

    @Query("select s from Story s left join fetch s.remoteAvatarMediaFile left join fetch s.remoteOwnerAvatarMediaFile"
            + " where s.nodeId = ?1 and s.id = ?2")
    Optional<Story> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2")
    List<Story> findByFeed(UUID nodeId, String feedName, Pageable pageable);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3"
            + " and s.entry.id = ?4")
    int countByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Query("select s from Story s left join fetch s.substories"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.entry.id = ?4"
            + " order by s.moment desc")
    List<Story> findFullByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Query("select s from Story s left join fetch s.remoteAvatarMediaFile left join fetch s.remoteOwnerAvatarMediaFile"
            + " left join fetch s.parent"
            + " where s.nodeId = ?1 and s.parent is not null and s.storyType = ?2 and s.entry.id = ?3"
            + " order by s.moment desc")
    List<Story> findSubsByTypeAndEntryId(UUID nodeId, StoryType storyType, UUID entryId);

    @Query("select s from Story s where s.nodeId = ?1 and s.entry.id = ?2 and s.parent is null order by s.moment desc")
    List<Story> findByEntryId(UUID nodeId, UUID entryId);

    @Modifying
    @Query("delete from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.entry.id = ?4")
    void deleteByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Modifying
    @Query("delete from Story s"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.remoteNodeName = ?4"
            + " and s.remotePostingId = ?5 and s.remoteCommentId = ?6")
    void deleteByRemotePostingAndCommentId(UUID nodeId, String feedName, StoryType storyType, String remoteNodeName,
                                           String remotePostingId, String remoteCommentId);

    @Modifying
    @Query("delete from Story s"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.remoteNodeName = ?4"
            + " and s.remotePostingId = ?5")
    void deleteByRemotePostingId(UUID nodeId, String feedName, StoryType storyType, String remoteNodeName,
                                           String remotePostingId);

    @Query("select s.moment from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    List<Long> findSliceAdmin(UUID nodeId, String feedName, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select s.moment from Story s"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4 and s.entry.ownerName = ?5"
            + " and s.entry.viewPrincipal = 'private'")
    List<Long> findSlicePrivate(UUID nodeId, String feedName, long afterMoment, long beforeMoment, String ownerName,
                                Pageable pageable);

    @Query("select s.moment from Story s left join s.entry e"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4"
            + " and e.viewPrincipal in (?5)")
    List<Long> findSliceNotAdmin(UUID nodeId, String feedName, long afterMoment, long beforeMoment,
                                 Collection<Principal> ownerName, Pageable pageable);

    @Query("select s from Story s"
            + " left join fetch s.remoteAvatarMediaFile left join fetch s.remoteOwnerAvatarMediaFile"
            + " left join fetch s.entry e left join fetch e.currentRevision cr left join fetch e.reactionTotals"
            + " left join fetch e.sources left join fetch e.ownerAvatarMediaFile"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    Set<Story> findInRange(UUID nodeId, String feedName, long afterMoment, long beforeMoment);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2")
    int countInFeed(UUID nodeId, String feedName);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment = ?3")
    int countMoments(UUID nodeId, String feedName, long moment);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    int countInRange(UUID nodeId, String feedName, long afterMoment, long beforeMoment);

    @Query("select s.moment from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    Page<Long> findMomentsInRange(UUID nodeId, String feedName, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.pinned = true")
    int countPinned(UUID nodeId, String feedName);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.viewed = false")
    int countNotViewed(UUID nodeId, String feedName);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.read = false")
    int countNotRead(UUID nodeId, String feedName);

    @Query("select min(s.moment) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.viewed = false")
    Long findNotViewedMoment(UUID nodeId, String feedName);

    @Query("select min(s.moment) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.read = false")
    Long findNotReadMoment(UUID nodeId, String feedName);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment <= ?4 and s.viewed = ?3")
    Set<Story> findViewed(UUID nodeId, String feedName, boolean viewed, long beforeMoment);

    @Modifying
    @Query("update Story s set s.viewed = ?3"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment <= ?4 and s.viewed = ?5")
    void updateViewed(UUID nodeId, String feedName, boolean viewed, long beforeMoment, boolean currentViewed);

    @Modifying
    @Query("update Story s set s.read = ?3"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment <= ?4 and s.read = ?5")
    void updateRead(UUID nodeId, String feedName, boolean read, long beforeMoment, boolean currentRead);

    @Query("select max(s.moment) from Story s where s.nodeId = ?1 and s.feedName = ?2")
    Long findLastMoment(UUID nodeId, String feedName);

    @Query("select max(s.createdAt) from Story s where s.nodeId = ?1 and s.feedName = ?2")
    Timestamp findLastCreatedAt(UUID nodeId, String feedName);

    @Query("select min(s.createdAt) from Story s where s.nodeId = ?1 and s.feedName = ?2")
    Timestamp findFirstCreatedAt(UUID nodeId, String feedName);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3"
            + " and s.remoteNodeName = ?4 and s.remotePostingId = ?5 order by s.moment desc")
    List<Story> findByRemotePostingId(UUID nodeId, String feedName, StoryType storyType,
                                      String remoteNodeName, String remotePostingId);

    @Query("select s from Story s left join fetch s.substories"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.remoteNodeName = ?4"
            + " and s.remotePostingId = ?5 order by s.moment desc")
    List<Story> findFullByRemotePostingId(UUID nodeId, String feedName, StoryType storyType, String remoteNodeName,
                                          String remotePostingId);

    @Query("select s from Story s left join fetch s.substories"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.remoteNodeName = ?4"
            + " and s.remotePostingId = ?5 and s.remoteCommentId = ?6 order by s.moment desc")
    List<Story> findFullByRemotePostingAndCommentId(UUID nodeId, String feedName, StoryType storyType,
                                                    String remoteNodeName, String remotePostingId,
                                                    String remoteCommentId);

    @Query("select s from Story s left join fetch s.substories"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.remoteNodeName = ?4"
            + " and s.remotePostingId = ?5 and s.remoteRepliedToId = ?6 order by s.moment desc")
    List<Story> findFullByRemotePostingAndRepliedToId(UUID nodeId, String feedName, StoryType storyType,
                                                      String remoteNodeName, String remotePostingId,
                                                      String remoteRepliedToId);

    @Query("select s from Story s left join fetch s.parent"
            + " where s.nodeId = ?1 and s.parent is not null and s.storyType = ?2 and s.remoteNodeName = ?3"
            + " and s.remotePostingId = ?4 and s.remoteCommentId = ?5 order by s.moment desc")
    List<Story> findSubsByRemotePostingAndCommentId(UUID nodeId, StoryType storyType, String remoteNodeName,
                                                    String remotePostingId, String remoteCommentId);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.createdAt < ?3")
    List<Story> findExpired(UUID nodeId, String feedName, Timestamp createdBefore);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.viewed = true and s.createdAt < ?3")
    List<Story> findExpiredViewed(UUID nodeId, String feedName, Timestamp createdBefore);

}
