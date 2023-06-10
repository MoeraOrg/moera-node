ALTER TABLE sheriff_orders ADD COLUMN complain_group_id uuid;
CREATE INDEX sheriff_orders_complain_group_idx ON sheriff_complain_groups(id);
ALTER TABLE sheriff_orders ADD FOREIGN KEY (complain_group_id) REFERENCES sheriff_complain_groups(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
