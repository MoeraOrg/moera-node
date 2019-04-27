DELETE FROM tokens;
ALTER TABLE tokens ADD node_id uuid NOT NULL;
