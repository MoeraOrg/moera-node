ALTER TABLE picks ADD COLUMN pinned boolean DEFAULT false NOT NULL;
ALTER TABLE picks ADD COLUMN read boolean DEFAULT false NOT NULL;
ALTER TABLE picks ADD COLUMN satisfied boolean DEFAULT false NOT NULL;
