ALTER TABLE contacts ADD COLUMN remote_source_uri varchar(1024);

ALTER TABLE drafts ADD COLUMN owner_source_uri varchar(1024);

ALTER TABLE entries ADD COLUMN receiver_source_uri varchar(1024);
ALTER TABLE entries ADD COLUMN owner_source_uri varchar(1024);
ALTER TABLE entries ADD COLUMN replied_to_source_uri varchar(1024);

ALTER TABLE entry_sources ADD COLUMN remote_source_uri varchar(1024);

ALTER TABLE own_comments ADD COLUMN remote_source_uri varchar(1024);
ALTER TABLE own_comments ADD COLUMN remote_replied_to_source_uri varchar(1024);

ALTER TABLE own_postings ADD COLUMN remote_source_uri varchar(1024);

ALTER TABLE own_reactions ADD COLUMN remote_source_uri varchar(1024);

ALTER TABLE reactions ADD COLUMN owner_source_uri varchar(1024);

ALTER TABLE sheriff_complaints ADD COLUMN owner_source_uri varchar(1024);

ALTER TABLE sheriff_complaint_groups ADD COLUMN remote_node_source_uri varchar(1024);
ALTER TABLE sheriff_complaint_groups ADD COLUMN remote_posting_owner_source_uri varchar(1024);
ALTER TABLE sheriff_complaint_groups ADD COLUMN remote_comment_owner_source_uri varchar(1024);

ALTER TABLE sheriff_orders ADD COLUMN remote_node_source_uri varchar(1024);
ALTER TABLE sheriff_orders ADD COLUMN remote_posting_owner_source_uri varchar(1024);
ALTER TABLE sheriff_orders ADD COLUMN remote_comment_owner_source_uri varchar(1024);

ALTER TABLE stories ADD COLUMN remote_source_uri varchar(1024);
ALTER TABLE stories ADD COLUMN remote_posting_source_uri varchar(1024);
ALTER TABLE stories ADD COLUMN remote_owner_source_uri varchar(1024);
