ALTER TABLE entry_sharings ADD COLUMN moment bigint;

-- Preserve chronological order and give equal timestamps distinct pagination moments.
WITH ordered AS (
    SELECT id, entry_id, floor(extract(epoch FROM created_at))::bigint * 1000 AS base,
           row_number() OVER (PARTITION BY entry_id ORDER BY created_at, id) AS position
    FROM entry_sharings
), moments AS (
    SELECT id, max(base - position) OVER (PARTITION BY entry_id ORDER BY position) + position AS moment
    FROM ordered
)
UPDATE entry_sharings s SET moment = m.moment FROM moments m WHERE s.id = m.id;

ALTER TABLE entry_sharings ALTER COLUMN moment SET NOT NULL;
CREATE UNIQUE INDEX ON entry_sharings(entry_id, moment);
