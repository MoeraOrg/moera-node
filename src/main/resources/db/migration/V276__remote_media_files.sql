CREATE TABLE remote_media_files (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name varchar(135) NOT NULL,
    media_id varchar(40) NOT NULL,
    hash varchar(40),
    digest bytea,
    mime_type varchar(80),
    attachment boolean NOT NULL DEFAULT false,
    size_x smallint,
    size_y smallint,
    file_size bigint,
    lease_id varchar(40),
    usage_count integer NOT NULL DEFAULT 0,
    deadline timestamp without time zone
);
CREATE INDEX remote_media_files_deadline_idx ON remote_media_files(deadline);
CREATE INDEX remote_media_files_node_id_lease_id_idx
    ON remote_media_files(node_id, lease_id) WHERE lease_id IS NOT NULL;
CREATE TRIGGER update_deadline BEFORE INSERT OR UPDATE OF usage_count, deadline ON remote_media_files
    FOR EACH ROW EXECUTE PROCEDURE update_media_file_deadline();

ALTER TABLE entry_attachments ADD COLUMN remote_media_file_id uuid;

CREATE FUNCTION update_remote_media_file_reference(old_id uuid, new_id uuid) RETURNS void
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF old_id = new_id THEN
            RETURN;
        END IF;
        IF old_id IS NOT NULL THEN
            UPDATE remote_media_files SET usage_count = usage_count - 1 WHERE id = old_id;
        END IF;
        IF new_id IS NOT NULL THEN
            UPDATE remote_media_files SET usage_count = usage_count + 1 WHERE id = new_id;
        END IF;
    END;
$$;
CREATE FUNCTION update_entry_attachments_remote_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_remote_media_file_reference(OLD.remote_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_remote_media_file_reference(OLD.remote_media_file_id, NEW.remote_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_remote_media_file_reference(NULL, NEW.remote_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;
CREATE TRIGGER update_remote_media_file_id
    AFTER INSERT OR UPDATE OF remote_media_file_id OR DELETE ON entry_attachments
    FOR EACH ROW EXECUTE PROCEDURE update_entry_attachments_remote_media_file_id();

WITH remote_media_refs AS (
    SELECT
        ea.id AS attachment_id,
        d.node_id AS node_id,
        d.receiver_name AS node_name,
        ea.remote_media_id AS media_id,
        COALESCE(ea.remote_media_hash, mf.id) AS hash,
        COALESCE(ea.remote_media_digest, mf.digest) AS digest,
        CASE
            WHEN mf.id IS NOT NULL THEN mf.mime_type
            WHEN ea.remote_media_hash IS NULL
                AND ea.remote_media_digest IS NULL
                AND ea.remote_media_mime_type = 'image/jpeg'
                AND ea.remote_media_attachment = false
                THEN NULL
            ELSE ea.remote_media_mime_type
        END AS mime_type,
        ea.remote_media_attachment AS attachment,
        mf.size_x AS size_x,
        mf.size_y AS size_y,
        mf.file_size AS file_size
    FROM entry_attachments ea
        JOIN drafts d ON d.id = ea.draft_id
        LEFT JOIN media_file_owners mfo ON mfo.id = ea.media_file_owner_id
        LEFT JOIN media_files mf ON mf.id = mfo.media_file_id
    WHERE ea.remote_media_id IS NOT NULL

    UNION ALL

    SELECT
        ea.id AS attachment_id,
        e.node_id AS node_id,
        COALESCE(es.remote_node_name, e.receiver_name) AS node_name,
        ea.remote_media_id AS media_id,
        COALESCE(ea.remote_media_hash, mf.id) AS hash,
        COALESCE(ea.remote_media_digest, mf.digest) AS digest,
        CASE
            WHEN mf.id IS NOT NULL THEN mf.mime_type
            WHEN ea.remote_media_hash IS NULL
                AND ea.remote_media_digest IS NULL
                AND ea.remote_media_mime_type = 'image/jpeg'
                AND ea.remote_media_attachment = false
                THEN NULL
            ELSE ea.remote_media_mime_type
        END AS mime_type,
        ea.remote_media_attachment AS attachment,
        mf.size_x AS size_x,
        mf.size_y AS size_y,
        mf.file_size AS file_size
    FROM entry_attachments ea
        JOIN entry_revisions er ON er.id = ea.entry_revision_id
        JOIN entries e ON e.id = er.entry_id
        LEFT JOIN LATERAL (
            SELECT entry_sources.remote_node_name
            FROM entry_sources
            WHERE entry_sources.entry_id = e.id
            ORDER BY entry_sources.created_at
            LIMIT 1
        ) es ON true
        LEFT JOIN media_file_owners mfo ON mfo.id = ea.media_file_owner_id
        LEFT JOIN media_files mf ON mf.id = mfo.media_file_id
    WHERE ea.remote_media_id IS NOT NULL
)
INSERT INTO remote_media_files(
    id, node_id, node_name, media_id, hash, digest, mime_type, attachment, size_x, size_y, file_size, lease_id
)
SELECT
    attachment_id, node_id, node_name, media_id, hash, digest, mime_type, attachment, size_x, size_y, file_size, NULL
FROM remote_media_refs
WHERE node_name IS NOT NULL;

UPDATE entry_attachments ea
SET remote_media_file_id = rmf.id
FROM remote_media_files rmf
WHERE rmf.id = ea.id;

ALTER TABLE entry_attachments ADD FOREIGN KEY (remote_media_file_id)
    REFERENCES remote_media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX entry_attachments_remote_media_file_id_idx ON entry_attachments(remote_media_file_id);

ALTER TABLE entry_attachments DROP COLUMN remote_media_id;
ALTER TABLE entry_attachments DROP COLUMN remote_media_hash;
ALTER TABLE entry_attachments DROP COLUMN remote_media_digest;
ALTER TABLE entry_attachments DROP COLUMN remote_media_mime_type;
ALTER TABLE entry_attachments DROP COLUMN remote_media_attachment;
