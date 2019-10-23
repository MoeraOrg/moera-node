UPDATE entries SET owner_name = CONCAT(owner_name, '_', owner_generation) WHERE owner_name IS NOT NULL;
ALTER TABLE entries DROP COLUMN owner_generation;
