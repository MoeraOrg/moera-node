ALTER TABLE sheriff_orders ADD COLUMN moment bigint NOT NULL DEFAULT 0;
CREATE INDEX ON sheriff_orders(node_id, moment);
UPDATE sheriff_orders SET moment = (EXTRACT(EPOCH FROM created_at) * 1000)::bigint;
