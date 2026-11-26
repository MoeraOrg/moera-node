ALTER TABLE linked_entries ADD COLUMN sharing_moment bigint;

CREATE UNIQUE INDEX linked_entries_sharing_moment_idx ON linked_entries(linked_entry_id, sharing_moment DESC)
    WHERE sharing_moment IS NOT NULL;
