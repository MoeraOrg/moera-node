package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SearchEngineStatisticsRepository extends JpaRepository<SearchEngineStatistics, UUID> {

    @Query("select new org.moera.node.data.SearchEngineClicks("
            + "st.nodeName, st.postingId, st.commentId, st.mediaId, max(st.heading), count(*) as clicks,"
            + " max(st.clickedAt) as lastClickedAt"
            + ")"
            + " from SearchEngineStatistics st"
            + " where st.ownerName = ?1 and st.clickedAt >= ?2 and st.clickedAt < ?3"
            + " group by st.nodeName, st.postingId, st.commentId, st.mediaId"
            + " order by clicks desc, lastClickedAt desc")
    List<SearchEngineClicks> calculateClicks(String ownerName, Timestamp fromTime, Timestamp tillTime,
                                             Pageable pageable);

    @Modifying
    @Query("delete SearchEngineStatistics st where st.clickedAt < ?1")
    void deleteOutdated(Timestamp clickedBefore);

}
