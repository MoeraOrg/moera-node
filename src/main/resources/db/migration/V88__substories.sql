ALTER TABLE stories ALTER COLUMN feed_name DROP NOT NULL;
ALTER TABLE stories ADD COLUMN parent_id uuid;
CREATE INDEX ON stories(parent_id);
ALTER TABLE stories ADD FOREIGN KEY (parent_id) REFERENCES stories(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE stories RENAME remote_entry_id TO remote_posting_id;
ALTER TABLE stories ADD COLUMN remote_comment_id varchar(40);
DROP TABLE stories_reactions;
