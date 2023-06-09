ALTER TABLE sheriff_complains ADD COLUMN anonymous_requested boolean NOT NULL DEFAULT false;
ALTER TABLE sheriff_complain_groups ADD COLUMN anonymous boolean NOT NULL DEFAULT false;
