CREATE TABLE option_defaults (
    id uuid NOT NULL PRIMARY KEY,
    name varchar(128) NOT NULL,
    value varchar(4096),
    privileged boolean
);
CREATE UNIQUE INDEX option_defaults_name_idx ON option_defaults(name);
