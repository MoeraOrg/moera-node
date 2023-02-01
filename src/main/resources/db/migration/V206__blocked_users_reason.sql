ALTER TABLE blocked_users ADD COLUMN reason_src text NOT NULL DEFAULT '';
ALTER TABLE blocked_users ADD COLUMN reason_src_format smallint NOT NULL DEFAULT 0;
ALTER TABLE blocked_users ADD COLUMN reason text NOT NULL DEFAULT '';
ALTER TABLE blocked_by_users ADD COLUMN reason text NOT NULL DEFAULT '';
