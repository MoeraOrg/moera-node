ALTER TABLE picks ADD COLUMN posting_view_principal varchar(70);
ALTER TABLE picks ADD COLUMN allow_multiple boolean DEFAULT false NOT NULL;
