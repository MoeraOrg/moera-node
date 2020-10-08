CREATE TABLE own_comments (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    remote_posting_id varchar(40) NOT NULL,
    remote_comment_id varchar(40) NOT NULL,
    heading varchar(255) NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX ON own_comments(node_id, remote_node_name, remote_posting_id, remote_comment_id);
