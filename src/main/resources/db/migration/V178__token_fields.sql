ALTER TABLE tokens ADD COLUMN id uuid NOT NULL DEFAULT uuid_generate_v4();
ALTER TABLE tokens DROP CONSTRAINT tokens_pkey;
ALTER TABLE tokens ADD PRIMARY KEY(id);
CREATE UNIQUE INDEX tokens_token_idx ON tokens(token);
ALTER TABLE tokens ADD COLUMN ip inet;
ALTER TABLE tokens ADD COLUMN plugin_name varchar(48);
ALTER TABLE tokens ALTER COLUMN deadline DROP NOT NULL;
