DROP TRIGGER update_remote_node ON blocked_by_users;
DROP TRIGGER update_remote_node ON blocked_users;
DROP TRIGGER update_remote_node ON contacts;
DROP TRIGGER update_remote_node ON entries;
DROP TRIGGER update_remote_node ON friend_ofs;
DROP TRIGGER update_remote_node ON friends;
DROP TRIGGER update_remote_node ON subscribers;

ALTER TABLE ask_history
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE blocked_by_users
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE blocked_instants
    ALTER COLUMN remote_node_name TYPE varchar(135),
    ALTER COLUMN remote_owner_name TYPE varchar(135);

ALTER TABLE blocked_users
    ALTER COLUMN remote_node_name TYPE varchar(135),
    ALTER COLUMN entry_node_name TYPE varchar(135);

ALTER TABLE contact_upgrades
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE contacts
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE drafts
    ALTER COLUMN receiver_name TYPE varchar(135);

ALTER TABLE entries
    ALTER COLUMN receiver_name TYPE varchar(135),
    ALTER COLUMN owner_name TYPE varchar(135),
    ALTER COLUMN replied_to_name TYPE varchar(135);

ALTER TABLE entry_sources
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE favors
    ALTER COLUMN node_name TYPE varchar(135);

ALTER TABLE friend_ofs
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE friends
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE grants
    ALTER COLUMN node_name TYPE varchar(135);

ALTER TABLE initial_recommendations
    ALTER COLUMN node_name TYPE varchar(135);

ALTER TABLE media_file_owners
    ALTER COLUMN owner_name TYPE varchar(135);

ALTER TABLE own_comments
    ALTER COLUMN remote_node_name TYPE varchar(135),
    ALTER COLUMN remote_replied_to_name TYPE varchar(135);

ALTER TABLE own_postings
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE own_reactions
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE pending_notifications
    ALTER COLUMN node_name TYPE varchar(135);

ALTER TABLE picks
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE reactions
    ALTER COLUMN owner_name TYPE varchar(135);

ALTER TABLE remote_connectivity
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE remote_grants
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE remote_media_cache
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE remote_user_list_items
    ALTER COLUMN list_node_name TYPE varchar(135),
    ALTER COLUMN node_name TYPE varchar(135);

ALTER TABLE remote_verifications
    ALTER COLUMN node_name TYPE varchar(135),
    ALTER COLUMN owner_name TYPE varchar(135);

ALTER TABLE search_engine_statistics
    ALTER COLUMN node_name TYPE varchar(135),
    ALTER COLUMN owner_name TYPE varchar(135);

ALTER TABLE sheriff_complaint_groups
    ALTER COLUMN remote_node_name TYPE varchar(135),
    ALTER COLUMN remote_posting_owner_name TYPE varchar(135),
    ALTER COLUMN remote_comment_owner_name TYPE varchar(135);

ALTER TABLE sheriff_complaints
    ALTER COLUMN owner_name TYPE varchar(135);

ALTER TABLE sheriff_orders
    ALTER COLUMN remote_node_name TYPE varchar(135),
    ALTER COLUMN remote_posting_owner_name TYPE varchar(135),
    ALTER COLUMN remote_comment_owner_name TYPE varchar(135);

ALTER TABLE stories
    ALTER COLUMN remote_node_name TYPE varchar(135),
    ALTER COLUMN remote_posting_node_name TYPE varchar(135),
    ALTER COLUMN remote_owner_name TYPE varchar(135);

ALTER TABLE subscribers
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE subscriptions
    ALTER COLUMN remote_node_name TYPE varchar(135);

ALTER TABLE user_list_items
    ALTER COLUMN node_name TYPE varchar(135);

ALTER TABLE user_subscriptions
    ALTER COLUMN remote_node_name TYPE varchar(135);

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
                old_receiver_name varchar(135) := CASE WHEN OLD.deleted_at IS NULL THEN OLD.receiver_name ELSE NULL END;
                old_receiver_entry_id varchar(40) := CASE WHEN OLD.deleted_at IS NULL THEN OLD.receiver_entry_id ELSE NULL END;
                new_receiver_name varchar(135) := CASE WHEN NEW.deleted_at IS NULL THEN NEW.receiver_name ELSE NULL END;
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

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON blocked_by_users FOR EACH ROW
EXECUTE FUNCTION update_blocked_by_user_remote_node();

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON blocked_users FOR EACH ROW
EXECUTE FUNCTION update_blocked_user_remote_node();

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON contacts FOR EACH ROW
EXECUTE FUNCTION update_contact_remote_node();

CREATE TRIGGER update_remote_node
AFTER INSERT OR DELETE OR UPDATE OF node_id, receiver_name, receiver_entry_id, deleted_at
ON entries FOR EACH ROW
EXECUTE FUNCTION update_entry_remote_node();

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON friend_ofs FOR EACH ROW
EXECUTE FUNCTION update_friend_of_remote_node();

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON friends FOR EACH ROW
EXECUTE FUNCTION update_friend_remote_node();

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name
ON subscribers FOR EACH ROW
EXECUTE FUNCTION update_subscriber_remote_node();
