CREATE TABLE public.remote_comment_verifications (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    receiver_name character varying(63),
    posting_id character varying(40) NOT NULL,
    comment_id character varying(40) NOT NULL,
    revision_id character varying(40),
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255),
    deadline timestamp without time zone NOT NULL
);
