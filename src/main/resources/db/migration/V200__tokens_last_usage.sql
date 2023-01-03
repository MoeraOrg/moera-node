ALTER TABLE tokens ADD COLUMN last_used_at timestamp without time zone;
ALTER TABLE tokens ADD COLUMN last_used_browser varchar(32);
ALTER TABLE tokens ADD COLUMN last_used_ip inet;
