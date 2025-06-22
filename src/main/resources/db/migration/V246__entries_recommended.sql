ALTER TABLE entries ADD COLUMN recommended boolean DEFAULT false NOT NULL;
ALTER TABLE picks ADD COLUMN recommended boolean DEFAULT false NOT NULL;
