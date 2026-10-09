ALTER TABLE tokens
    ADD COLUMN token_hash bytea,
    ADD COLUMN token_prefix character varying(4);

UPDATE tokens
SET token_hash = sha256(convert_to(token, 'UTF8')),
    token_prefix = left(token, 4);

ALTER TABLE tokens
    ALTER COLUMN token_hash SET NOT NULL,
    ALTER COLUMN token_prefix SET NOT NULL,
    DROP COLUMN token;

CREATE UNIQUE INDEX tokens_token_hash_idx ON tokens(token_hash);
