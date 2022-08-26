DELETE FROM sitemap_records
WHERE EXISTS (SELECT * FROM entries WHERE sitemap_records.entry_id=entries.id AND entries.receiver_name IS NOT NULL);
