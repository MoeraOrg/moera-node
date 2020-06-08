ALTER TABLE entries ALTER COLUMN receiver_name DROP NOT NULL;
UPDATE entries SET receiver_name = NULL;
