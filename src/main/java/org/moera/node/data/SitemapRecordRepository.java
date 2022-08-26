package org.moera.node.data;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SitemapRecordRepository extends JpaRepository<SitemapRecord, UUID> {

    @Query("select p from Story s left join s.entry p left join p.sitemapRecord r where p.nodeId = ?1"
            + " and s.feedName = 'timeline' and p is not null and (r is null or p.editedAt > r.modifiedAt)"
            + " and p.deletedAt is null")
    Collection<Posting> findUpdated(UUID nodeId);

    @Query("select new org.moera.node.data.Sitemap(r.sitemapId, count(*), max(r.modifiedAt)) from SitemapRecord r"
            + " where r.nodeId = ?1 group by r.sitemapId")
    Collection<Sitemap> findSitemaps(UUID nodeId);

    @Query("select r from SitemapRecord r where r.nodeId = ?1 and r.sitemapId = ?2 and r.visible = true")
    Collection<SitemapRecord> findRecords(UUID nodeId, UUID id);

}
