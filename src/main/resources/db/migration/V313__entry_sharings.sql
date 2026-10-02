ALTER TABLE entries ADD COLUMN total_sharings integer NOT NULL DEFAULT 0;
ALTER TABLE entries ADD COLUMN view_sharings_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_sharings_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_total_sharings_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_total_sharings_principal varchar(70);

UPDATE entries SET receiver_view_sharings_principal = 'public', receiver_view_total_sharings_principal = 'public'
WHERE receiver_name IS NOT NULL;

CREATE TABLE entry_sharings (
    id uuid PRIMARY KEY,
    sharing_type smallint NOT NULL,
    remote_node_name varchar(135) NOT NULL,
    remote_full_name varchar(96),
    remote_source_uri varchar(1024),
    remote_title varchar(120),
    remote_gender varchar(31),
    remote_avatar_media_file_id varchar(40)
        REFERENCES media_files(id) ON UPDATE CASCADE ON DELETE SET NULL,
    remote_avatar_shape varchar(8),
    remote_feed_name varchar(63),
    remote_story_id varchar(40),
    remote_story_moment bigint,
    remote_posting_id varchar(40),
    entry_id uuid NOT NULL REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE,
    entry_revision_id uuid REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    CHECK ((remote_feed_name IS NULL AND remote_story_id IS NULL AND remote_story_moment IS NULL)
        OR (remote_feed_name IS NOT NULL AND remote_story_id IS NOT NULL AND remote_story_moment IS NOT NULL)),
    CHECK (remote_posting_id IS NOT NULL OR remote_story_id IS NOT NULL)
);

CREATE INDEX ON entry_sharings(entry_id, created_at, id);
CREATE INDEX ON entry_sharings(entry_revision_id);
CREATE INDEX ON entry_sharings(remote_avatar_media_file_id);
CREATE INDEX ON entry_sharings(entry_id, sharing_type, remote_node_name);

CREATE TRIGGER update_remote_avatar_media_file_id
    AFTER INSERT OR UPDATE OF remote_avatar_media_file_id OR DELETE ON entry_sharings
    FOR EACH ROW EXECUTE PROCEDURE update_entity_remote_avatar_media_file_id();

-- Logical identity survives sharing deletion until the liberin removes the matching substory.
ALTER TABLE stories ADD COLUMN sharing_id uuid;
-- Keep the publication identity stable when the cached revision or the story moment changes.
ALTER TABLE stories ADD COLUMN shared_revision_id varchar(40);
ALTER TABLE stories ADD COLUMN shared_story_moment bigint;
