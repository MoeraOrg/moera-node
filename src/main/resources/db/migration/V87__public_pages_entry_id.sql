ALTER TABLE public_pages ADD COLUMN entry_id uuid;
ALTER TABLE public_pages ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
DROP INDEX public_pages_node_id_after_moment_idx;
DROP INDEX public_pages_node_id_before_moment_idx;
CREATE INDEX ON public_pages(node_id, entry_id, after_moment);
CREATE INDEX ON public_pages(node_id, entry_id, before_moment);
