DELETE FROM sitemap_records
WHERE (SELECT parent_media_id FROM entries WHERE sitemap_records.entry_id = id) IS NOT NULL;