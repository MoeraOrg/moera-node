ALTER TABLE entries ADD heading character varying(255) NOT NULL DEFAULT '';
UPDATE entries SET heading = body_html;
