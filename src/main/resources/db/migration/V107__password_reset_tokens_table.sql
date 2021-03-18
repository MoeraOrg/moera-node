CREATE TABLE password_reset_tokens (
    token varchar(10) NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX ON password_reset_tokens(node_id);
CREATE INDEX ON password_reset_tokens(deadline);
