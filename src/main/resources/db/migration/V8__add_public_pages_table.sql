CREATE TABLE public_pages (
    id bigint NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    begin_moment bigint NOT NULL,
    end_moment bigint NOT NULL
);
CREATE INDEX ON public_pages(node_id);
CREATE INDEX ON public_pages(begin_moment);
CREATE INDEX ON public_pages(end_moment);
