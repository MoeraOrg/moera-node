ALTER TABLE favors ADD COLUMN favor_type smallint;

UPDATE favors
SET favor_type = CASE
    WHEN value = 10 AND decay_hours = 14 * 24 THEN 0
    WHEN value = 1 AND decay_hours = 3 * 24 THEN 1
    WHEN value = 10 AND decay_hours = 7 * 24 THEN 2
    WHEN value = 0.25 AND decay_hours = 3 * 24 THEN 3
    WHEN value = 5 AND decay_hours = 7 * 24 THEN 4
    WHEN value = -10 AND decay_hours = 14 * 24 THEN 6
    WHEN value = -1 AND decay_hours = 3 * 24 THEN 7
    WHEN value = -10 AND decay_hours = 7 * 24 THEN 8
    WHEN value = -0.25 AND decay_hours = 3 * 24 THEN 9
    WHEN value = -5 AND decay_hours = 7 * 24 THEN 10
    ELSE 0
END;

ALTER TABLE favors ALTER COLUMN favor_type SET NOT NULL;
CREATE INDEX favors_node_id_type_name_deadline_idx ON favors(node_id, favor_type, node_name, deadline);

ALTER TABLE contacts ADD COLUMN visit_count integer NOT NULL DEFAULT 0;
CREATE INDEX contacts_node_id_visited_distance_idx ON contacts(node_id, distance) WHERE visit_count > 0;
