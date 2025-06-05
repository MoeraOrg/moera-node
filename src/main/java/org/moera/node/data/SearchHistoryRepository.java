package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    @Query("select sh from SearchHistory sh where nodeId = ?1 and query ilike ?2 order by createdAt desc")
    List<SearchHistory> findByQuery(UUID nodeId, String query, Pageable pageable);

    @Query("select sh from SearchHistory sh where nodeId = ?1 and query ilike ?2% order by createdAt desc")
    List<SearchHistory> findByPrefix(UUID nodeId, String prefix, Pageable pageable);

    @Query("delete from SearchHistory sh where sh.nodeId = ?1 and sh.query = ?2")
    @Modifying
    void deleteByQuery(UUID nodeId, String query);

    @Query("delete from SearchHistory sh where sh.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
