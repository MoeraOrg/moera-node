package org.moera.node.data;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SitemapRecordRepository extends JpaRepository<SitemapRecord, UUID> {

    @Query(value = "select p.* from stories s"
            + " left join entries p on s.entry_id=p.id"
            + " left join sitemap_records r on p.id=r.entry_id"
            + " where p.node_id=?1 and s.feed_name = 'timeline' and p is not null"
            + " and (r is null or p.edited_at > r.modified_at) and p.deleted_at is null",
            nativeQuery = true)
    Collection<Posting> findUpdated(UUID nodeId);

    @Query("select r from SitemapRecord r where r.nodeId = ?1 and r.entry.id = ?2")
    SitemapRecord findByEntryId(UUID nodeId, UUID entryId);

    @Query("select new org.moera.node.data.Sitemap(r.sitemapId, count(*), max(r.modifiedAt)) from SitemapRecord r"
            + " where r.nodeId = ?1 group by r.sitemapId")
    Collection<Sitemap> findSitemaps(UUID nodeId);

    @Query("select r from SitemapRecord r where r.nodeId = ?1 and r.sitemapId = ?2 and r.visible = true")
    Collection<SitemapRecord> findRecords(UUID nodeId, UUID id);

}
