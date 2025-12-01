package org.moera.node.data;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SitemapRecordRepository extends JpaRepository<SitemapRecord, UUID> {

    @Query(
        "select p from Posting p"
        + " where p.nodeId = ?1"
        + " and exists(select s from Story s where s.entry.id = p.id and s.feedName = 'timeline')"
        + " and not exists(select r from SitemapRecord r where r.entry.id = p.id and r.modifiedAt >= p.editedAt)"
    )
    Collection<Posting> findUpdated(UUID nodeId);

    @Query("select r from SitemapRecord r where r.nodeId = ?1 and r.entry.id = ?2")
    SitemapRecord findByEntryId(UUID nodeId, UUID entryId);

    @Query(
        "select new org.moera.node.data.Sitemap(r.sitemapId, count(*), max(r.modifiedAt)) from SitemapRecord r"
        + " where r.nodeId = ?1 group by r.sitemapId"
    )
    Collection<Sitemap> findSitemaps(UUID nodeId);

    @Query("select r from SitemapRecord r where r.nodeId = ?1 and r.sitemapId = ?2 and r.visible = true")
    Collection<SitemapRecord> findRecords(UUID nodeId, UUID id);

}
