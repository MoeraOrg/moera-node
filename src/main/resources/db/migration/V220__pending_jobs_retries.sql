ALTER TABLE pending_jobs ADD COLUMN retries integer NOT NULL;
CREATE INDEX pending_jobs_wait_until_idx ON pending_jobs(wait_until);
