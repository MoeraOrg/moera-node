ALTER TABLE stories DROP COLUMN shared_revision_id;

UPDATE stories SET sharing_id = NULL
WHERE sharing_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM entry_sharings WHERE id = stories.sharing_id);

ALTER TABLE stories ADD FOREIGN KEY (sharing_id)
    REFERENCES entry_sharings(id) ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX ON stories(sharing_id);
