CREATE TABLE search_engine_statistics (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    engine smallint NOT NULL,
    owner_name varchar(63) NOT NULL,
    posting_id uuid,
    comment_id uuid,
    media_id uuid,
    clicked_at timestamp without time zone NOT NULL
);
CREATE INDEX search_engine_statistics_owner_clicked_idx ON search_engine_statistics(owner_name, clicked_at);
