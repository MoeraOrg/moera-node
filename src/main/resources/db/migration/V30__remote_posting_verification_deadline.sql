DELETE FROM remote_posting_verifications;
ALTER TABLE remote_posting_verifications ADD deadline timestamp without time zone NOT NULL;
CREATE INDEX ON remote_posting_verifications(deadline);
