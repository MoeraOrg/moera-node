CREATE TABLE pending_jobs (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid,
    job_type character varying(63) NOT NULL,
    parameters TEXT NOT NULL,
    state TEXT,
    created_at timestamp without time zone NOT NULL,
    wait_until timestamp without time zone
);
