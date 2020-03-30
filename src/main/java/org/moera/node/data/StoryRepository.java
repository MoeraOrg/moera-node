package org.moera.node.data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    @Query("select s from Story s where s.nodeId = ?1 and s.feedName = ?2 and s.storyType = ?3 and s.entry.id = ?4")
    Story findByFeedAndTypeAndEntryId(UUID nodeId, String feedName, StoryType storyType, UUID entryId);

    @Query("select s from Story s where s.nodeId = ?1 and s.entry.id = ?2 order by s.moment desc")
    List<Story> findByEntryId(UUID nodeId, UUID entryId);

    @Query("select s from Story s"
            + " left join fetch s.entry e left join fetch e.currentRevision left join fetch e.reactionTotals"
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

}
