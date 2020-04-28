ALTER TABLE stories ADD COLUMN tracking_id uuid NOT NULL DEFAULT uuid_generate_v4();
CREATE UNIQUE INDEX ON stories(tracking_id);
