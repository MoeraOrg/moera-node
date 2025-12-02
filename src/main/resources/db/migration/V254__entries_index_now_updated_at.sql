ALTER TABLE entries ADD COLUMN index_now_updated_at timestamp without time zone NOT NULL DEFAULT '1970-01-01 00:00:00';
CREATE INDEX entries_index_now_edited_at_idx ON entries (edited_at DESC)
    WHERE edited_at > index_now_updated_at OR deleted_at IS NOT NULL AND deleted_at > index_now_updated_at;
