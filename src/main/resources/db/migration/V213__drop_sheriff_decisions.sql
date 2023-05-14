ALTER TABLE sheriff_complain_groups ADD COLUMN decision_code smallint;
ALTER TABLE sheriff_complain_groups ADD COLUMN decision_details text;
ALTER TABLE sheriff_complain_groups ADD COLUMN decided_at timestamp without time zone;
ALTER TABLE sheriff_complain_groups DROP COLUMN sheriff_decision_id;
DROP TABLE sheriff_decisions;
