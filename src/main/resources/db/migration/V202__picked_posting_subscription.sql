CREATE OR REPLACE FUNCTION update_entry_remote_node() RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            IF OLD.receiver_name IS NOT NULL AND OLD.deleted_at IS NULL THEN
                PERFORM update_subscription_reference(
                    OLD.node_id, 1, OLD.receiver_name, NULL, OLD.receiver_entry_id, NULL, NULL, NULL
                );
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            DECLARE
                old_receiver_name varchar(63) := CASE WHEN OLD.deleted_at IS NULL THEN OLD.receiver_name ELSE NULL END;
                old_receiver_entry_id varchar(40) := CASE WHEN OLD.deleted_at IS NULL THEN OLD.receiver_entry_id ELSE NULL END;
                new_receiver_name varchar(63) := CASE WHEN NEW.deleted_at IS NULL THEN NEW.receiver_name ELSE NULL END;
                new_receiver_entry_id varchar(40) := CASE WHEN NEW.deleted_at IS NULL THEN NEW.receiver_entry_id ELSE NULL END;
            BEGIN
                IF (old_receiver_name IS NULL AND new_receiver_name IS NULL OR old_receiver_name = new_receiver_name)
                   AND (old_receiver_entry_id IS NULL AND new_receiver_entry_id IS NULL
                        OR old_receiver_entry_id = new_receiver_entry_id) THEN
                    RETURN NEW;
                END IF;
                IF new_receiver_name IS NULL THEN
                    PERFORM update_subscription_reference(
                        NEW.node_id, 1, old_receiver_name, NULL, old_receiver_entry_id, NULL, NULL, NULL
                    );
                ELSIF old_receiver_name IS NULL THEN
                    PERFORM update_subscription_reference(
                        NEW.node_id, 1, NULL, NULL, NULL, new_receiver_name, NULL, new_receiver_entry_id
                    );
                ELSE
                    PERFORM update_subscription_reference(
                        NEW.node_id, 1, old_receiver_name, NULL, old_receiver_entry_id,
                        new_receiver_name, NULL, new_receiver_entry_id
                    );
                END IF;
                RETURN NEW;
            END;
        ELSIF TG_OP = 'INSERT' THEN
            IF NEW.receiver_name IS NOT NULL AND NEW.deleted_at IS NULL THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 1, NULL, NULL, NULL, NEW.receiver_name, NULL, NEW.receiver_entry_id
                );
            END IF;
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_remote_node ON entries;
CREATE TRIGGER update_remote_node
AFTER INSERT OR DELETE OR UPDATE OF node_id, receiver_name, receiver_entry_id, deleted_at
ON entries FOR EACH ROW
EXECUTE FUNCTION update_entry_remote_node();
