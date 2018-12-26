CREATE TABLE tokens (
    token character varying(45) NOT NULL PRIMARY KEY,
    name character varying(127),
    generation integer,
    admin boolean NOT NULL,
    created timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
