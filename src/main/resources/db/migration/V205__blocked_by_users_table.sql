CREATE TABLE blocked_by_users (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    blocked_operation smallint NOT NULL,
    contact_id uuid,
    remote_node_name varchar(63) NOT NULL,
    remote_posting_id varchar(40),
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone
);
CREATE INDEX blocked_by_users_contact_id_idx ON blocked_by_users(contact_id);
ALTER TABLE blocked_by_users ADD FOREIGN KEY (contact_id) REFERENCES contacts(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
CREATE INDEX blocked_by_users_node_id_remote_idx ON blocked_by_users(node_id, remote_node_name, remote_posting_id);

ALTER TABLE contacts ADD COLUMN blocked_by_user_count integer NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN blocked_by_user_posting_count integer NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION update_blocked_by_user_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_subscription_reference(
                OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
            );
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
            );
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON blocked_by_users FOR EACH ROW
EXECUTE FUNCTION update_blocked_by_user_remote_node();
