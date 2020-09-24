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

public interface StoryRepository extends JpaRepository<Story, UUID> {

    @Query("select s from Story s where s.nodeId = ?1 and s.id = ?2")
    Optional<Story> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3"
            + " and s.entry.id = ?4")
    int countByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Query("select s from Story s left join fetch s.substories"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.entry.id = ?4"
            + " order by s.moment desc")
    List<Story> findFullByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Query("select s from Story s left join fetch s.parent"
            + " where s.nodeId = ?1 and s.parent is not null and s.storyType = ?2 and s.entry.id = ?3"
            + " order by s.moment desc")
    List<Story> findSubsByTypeAndEntryId(UUID nodeId, StoryType storyType, UUID entryId);

    @Query("select s from Story s where s.nodeId = ?1 and s.entry.id = ?2 order by s.moment desc")
    List<Story> findByEntryId(UUID nodeId, UUID entryId);

    @Modifying
    @Query("delete from Story s where s.nodeId = ?1 and s.entry.id = ?2")
    void deleteByEntryId(UUID nodeId, UUID entryId);

    @Modifying
    @Query("delete from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.entry.id = ?4")
    void deleteByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Query("select s from Story s left join fetch s.entry e"
            + " left join fetch e.currentRevision left join fetch e.reactionTotals left join fetch e.sources"
            + " where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    Set<Story> findInRange(UUID nodeId, String feedName, long afterMoment, long beforeMoment);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    Page<Story> findSlice(UUID nodeId, String feedName, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment = ?3")
    int countMoments(UUID nodeId, String feedName, long moment);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    int countInRange(UUID nodeId, String feedName, long afterMoment, long beforeMoment);

    @Query("select s.moment from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.moment > ?3 and s.moment <= ?4")
    Page<Long> findMomentsInRange(UUID nodeId, String feedName, long afterMoment, long beforeMoment, Pageable pageable);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.viewed = false")
    int countNotViewed(UUID nodeId, String feedName);

    @Query("select count(*) from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.read = false")
    int countNotRead(UUID nodeId, String feedName);

    @Modifying
    @Query("update Story s set s.viewed = ?3 where s.nodeId = ?1 and s.feedName = ?2 and s.moment <= ?4")
    void updateViewed(UUID nodeId, String feedName, boolean viewed, long beforeMoment);

    @Modifying
    @Query("update Story s set s.read = ?3 where s.nodeId = ?1 and s.feedName = ?2 and s.moment <= ?4")
    void updateRead(UUID nodeId, String feedName, boolean read, long beforeMoment);

    @Query("select s from Story s where s.nodeId = ?1 and s.trackingId = ?2")
    Optional<Story> findByTrackingId(UUID nodeId, UUID trackingId);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3"
            + " and s.remoteNodeName = ?4 order by s.moment desc")
    List<Story> findByRemoteNodeName(UUID nodeId, String feedName, StoryType storyType, String remoteNodeName);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3"
            + " and s.remoteNodeName = ?4 and s.remotePostingId = ?5 order by s.moment desc")
    List<Story> findByRemotePostingId(UUID nodeId, String feedName, StoryType storyType,
                                      String remoteNodeName, String remotePostingId);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3"
            + " and s.remoteNodeName = ?4 and s.remotePostingId = ?5 and s.remoteCommentId = ?6 order by s.moment desc")
    List<Story> findByRemotePostingAndCommentId(UUID nodeId, String feedName, StoryType storyType,
                                                String remoteNodeName, String remotePostingId, String remoteCommentId);

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.viewed = false and s.createdAt < ?3")
    List<Story> findExpired(UUID nodeId, String feedName, Timestamp createdBefore);

}
