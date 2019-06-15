DROP INDEX entries_moment_idx;
CREATE INDEX ON entries(node_id,moment);
DROP INDEX public_pages_begin_moment_idx;
DROP INDEX public_pages_end_moment_idx;
CREATE INDEX ON public_pages(node_id,begin_moment);
CREATE INDEX ON public_pages(node_id,end_moment);
