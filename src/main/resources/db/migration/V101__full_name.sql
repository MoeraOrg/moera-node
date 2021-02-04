ALTER TABLE entries ADD COLUMN owner_full_name varchar(96);
ALTER TABLE entries ADD COLUMN receiver_full_name varchar(96);
ALTER TABLE entries ADD COLUMN replied_to_full_name varchar(96);
ALTER TABLE entry_sources ADD COLUMN remote_full_name varchar(96);
ALTER TABLE own_comments ADD COLUMN remote_full_name varchar(96);
ALTER TABLE own_reactions ADD COLUMN remote_full_name varchar(96);
ALTER TABLE reactions ADD COLUMN owner_full_name varchar(96);
ALTER TABLE stories ADD COLUMN remote_full_name varchar(96);
ALTER TABLE stories ADD COLUMN remote_owner_full_name varchar(96);
ALTER TABLE subscribers ADD COLUMN remote_full_name varchar(96);
ALTER TABLE subscriptions ADD COLUMN remote_full_name varchar(96);
