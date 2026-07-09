CREATE TABLE media_uploads (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    mime_type varchar(80) NOT NULL,
    title varchar(255),
    file_size integer NOT NULL,
    chunk_size integer NOT NULL,
    uploaded_chunks integer[] NOT NULL,
    deadline timestamp without time zone NOT NULL,
    completed_at timestamp without time zone
);
CREATE INDEX media_uploads_node_id_deadline_idx ON media_uploads(node_id, deadline);
CREATE INDEX media_uploads_deadline_idx ON media_uploads(deadline);
