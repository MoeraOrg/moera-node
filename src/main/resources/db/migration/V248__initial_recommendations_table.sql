CREATE TABLE initial_recommendations (
    id uuid NOT NULL PRIMARY KEY,
    node_name varchar(63) NOT NULL,
    posting_id varchar(40) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE UNIQUE INDEX initial_recommendations_name_posting_idx ON initial_recommendations(node_name, posting_id);
CREATE INDEX initial_recommendations_deadline_idx ON initial_recommendations(deadline);
