ALTER TABLE entries ADD COLUMN receiver_sheriffs text;
ALTER TABLE entries ALTER COLUMN receiver_sheriff_marks DROP NOT NULL;
ALTER TABLE entries ALTER COLUMN receiver_sheriff_marks DROP DEFAULT;
