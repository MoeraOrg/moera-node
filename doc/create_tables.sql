--
-- PostgreSQL database dump
--

-- Dumped from database version 14.8 (Ubuntu 14.8-0ubuntu0.22.04.1)
-- Dumped by pg_dump version 14.8 (Ubuntu 14.8-0ubuntu0.22.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: update_blocked_by_user_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_blocked_by_user_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_blocked_by_user_remote_node() OWNER TO moera;

--
-- Name: update_blocked_user_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_blocked_user_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_blocked_user_remote_node() OWNER TO moera;

--
-- Name: update_contact_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_contact_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_contact_remote_node() OWNER TO moera;

--
-- Name: update_entity_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.media_file_id, NEW.media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_media_file_id() OWNER TO moera;

--
-- Name: update_entity_owner_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_owner_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.owner_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.owner_avatar_media_file_id, NEW.owner_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.owner_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_owner_avatar_media_file_id() OWNER TO moera;

--
-- Name: update_entity_receiver_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_receiver_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.receiver_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.receiver_avatar_media_file_id, NEW.receiver_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.receiver_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_receiver_avatar_media_file_id() OWNER TO moera;

--
-- Name: update_entity_remote_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_remote_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_avatar_media_file_id, NEW.remote_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_remote_avatar_media_file_id() OWNER TO moera;

--
-- Name: update_entity_remote_owner_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_remote_owner_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_owner_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_owner_avatar_media_file_id, NEW.remote_owner_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_owner_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_remote_owner_avatar_media_file_id() OWNER TO moera;

--
-- Name: update_entity_remote_posting_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_entity_remote_posting_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_posting_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_posting_avatar_media_file_id,
                                                NEW.remote_posting_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_posting_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_remote_posting_avatar_media_file_id() OWNER TO postgres;

--
-- Name: update_entity_remote_replied_to_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_remote_replied_to_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.remote_replied_to_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.remote_replied_to_avatar_media_file_id, NEW.remote_replied_to_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.remote_replied_to_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_remote_replied_to_avatar_media_file_id() OWNER TO moera;

--
-- Name: update_entity_replied_to_avatar_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entity_replied_to_avatar_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(OLD.replied_to_avatar_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(OLD.replied_to_avatar_media_file_id, NEW.replied_to_avatar_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, NEW.replied_to_avatar_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entity_replied_to_avatar_media_file_id() OWNER TO moera;

--
-- Name: update_entry_attachments_media_file_owner_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entry_attachments_media_file_owner_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_owner_reference(OLD.media_file_owner_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_owner_reference(OLD.media_file_owner_id, NEW.media_file_owner_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_owner_reference(NULL, NEW.media_file_owner_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_entry_attachments_media_file_owner_id() OWNER TO moera;

--
-- Name: update_entry_media_file_owner_usage(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entry_media_file_owner_usage() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        UPDATE media_file_owners
        SET usage_updated_at = now()
        WHERE id IN (
            SELECT media_file_owner_id
            FROM entry_attachments
            WHERE entry_attachments.entry_revision_id = OLD.current_revision_id
             OR entry_attachments.entry_revision_id = NEW.current_revision_id
        );
        RETURN NEW;
    END;
$$;


ALTER FUNCTION public.update_entry_media_file_owner_usage() OWNER TO moera;

--
-- Name: update_entry_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_entry_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_entry_remote_node() OWNER TO moera;

--
-- Name: update_friend_of_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_friend_of_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_friend_of_remote_node() OWNER TO moera;

--
-- Name: update_friend_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_friend_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_friend_remote_node() OWNER TO moera;

--
-- Name: update_media_file_deadline(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_media_file_deadline() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF NEW.usage_count < 0 THEN
            NEW.usage_count := 0;
        END IF;
        IF NEW.usage_count = 0 AND NEW.deadline IS NULL THEN
            NEW.deadline := NOW() + interval '1 day';
        ELSIF NEW.usage_count > 0 AND NEW.deadline IS NOT NULL THEN
            NEW.deadline := NULL;
        END IF;
        RETURN NEW;
    END;
$$;


ALTER FUNCTION public.update_media_file_deadline() OWNER TO moera;

--
-- Name: update_media_file_owner_reference(uuid, uuid); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_media_file_owner_reference(old_id uuid, new_id uuid) RETURNS void
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF old_id = new_id THEN
            RETURN;
        END IF;
        IF old_id IS NOT NULL THEN
            UPDATE media_file_owners SET usage_count = usage_count - 1, usage_updated_at = now() WHERE id = old_id;
        END IF;
        IF new_id IS NOT NULL THEN
            UPDATE media_file_owners SET usage_count = usage_count + 1, usage_updated_at = now() WHERE id = new_id;
        END IF;
    END;
$$;


ALTER FUNCTION public.update_media_file_owner_reference(old_id uuid, new_id uuid) OWNER TO moera;

--
-- Name: update_media_file_preview_media_file_id(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_media_file_preview_media_file_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    DECLARE
        old_media_file_id varchar(40) := NULL;
        new_media_file_id varchar(40) := NULL;
    BEGIN
        IF TG_OP = 'DELETE' OR TG_OP = 'UPDATE' THEN
            IF OLD.media_file_id <> OLD.original_media_file_id THEN
                old_media_file_id := OLD.media_file_id;
            END IF;
        END IF;
        IF TG_OP = 'UPDATE' OR TG_OP = 'INSERT' THEN
            IF NEW.media_file_id <> NEW.original_media_file_id THEN
                new_media_file_id := NEW.media_file_id;
            END IF;
        END IF;
        IF TG_OP = 'DELETE' THEN
            PERFORM update_media_file_reference(old_media_file_id, NULL);
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_media_file_reference(old_media_file_id, new_media_file_id);
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_media_file_reference(NULL, new_media_file_id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_media_file_preview_media_file_id() OWNER TO moera;

--
-- Name: update_media_file_reference(character varying, character varying); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_media_file_reference(old_id character varying, new_id character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF old_id = new_id THEN
            RETURN;
        END IF;
        IF old_id IS NOT NULL THEN
            UPDATE media_files SET usage_count = usage_count - 1 WHERE id = old_id;
        END IF;
        IF new_id IS NOT NULL THEN
            UPDATE media_files SET usage_count = usage_count + 1 WHERE id = new_id;
        END IF;
    END;
$$;


ALTER FUNCTION public.update_media_file_reference(old_id character varying, new_id character varying) OWNER TO moera;

--
-- Name: update_subscriber_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_subscriber_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            IF OLD.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
                );
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_subscriber_remote_node() OWNER TO moera;

--
-- Name: update_subscription_reference(uuid, integer, character varying, character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_subscription_reference(s_node_id uuid, s_type integer, old_node_name character varying, old_feed_name character varying, old_entry_id character varying, new_node_name character varying, new_feed_name character varying, new_entry_id character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF s_node_id IS NULL
           OR s_type IS NULL
           OR (
               old_node_name = new_node_name
               AND (old_feed_name IS NULL AND new_feed_name IS NULL OR old_feed_name = new_feed_name)
               AND (old_entry_id IS NULL AND new_entry_id IS NULL OR old_entry_id = new_entry_id)
           ) THEN
            RETURN;
        END IF;
        IF old_node_name IS NOT NULL THEN
            UPDATE subscriptions
            SET usage_count = usage_count - 1
            WHERE node_id = s_node_id AND subscription_type = s_type AND remote_node_name = old_node_name
                  AND (remote_feed_name IS NULL OR remote_feed_name = old_feed_name)
                  AND (remote_entry_id IS NULL OR remote_entry_id = old_entry_id) AND usage_count > 0;
        END IF;
        IF new_node_name IS NOT NULL THEN
            INSERT INTO subscriptions AS s(id, node_id, subscription_type, remote_node_name, remote_feed_name,
                                           remote_entry_id, created_at, status, usage_count)
            VALUES (uuid_generate_v4(), s_node_id, s_type, new_node_name, new_feed_name, new_entry_id, now(), 1, 1)
            ON CONFLICT (node_id, subscription_type, remote_node_name, coalesce(remote_feed_name, ''),
                         coalesce(remote_entry_id, ''))
            WHERE usage_count > 0
            DO UPDATE
            SET usage_count = s.usage_count + 1
            WHERE s.node_id = s_node_id AND s.subscription_type = s_type AND s.remote_node_name = old_node_name
                  AND (s.remote_feed_name IS NULL OR s.remote_feed_name = old_feed_name)
                  AND (s.remote_entry_id IS NULL OR s.remote_entry_id = old_entry_id) AND s.usage_count > 0;
        END IF;
    END;
$$;


ALTER FUNCTION public.update_subscription_reference(s_node_id uuid, s_type integer, old_node_name character varying, old_feed_name character varying, old_entry_id character varying, new_node_name character varying, new_feed_name character varying, new_entry_id character varying) OWNER TO moera;

--
-- Name: update_user_subscription_remote_node(); Type: FUNCTION; Schema: public; Owner: moera
--

CREATE FUNCTION public.update_user_subscription_remote_node() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM update_subscription_reference(
                OLD.node_id, OLD.subscription_type, OLD.remote_node_name, OLD.remote_feed_name, OLD.remote_entry_id,
                NULL, NULL, NULL
            );
            IF OLD.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    OLD.node_id, 3, OLD.remote_node_name, NULL, NULL, NULL, NULL, NULL
                );
            END IF;
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, NEW.subscription_type, OLD.remote_node_name, OLD.remote_feed_name, OLD.remote_entry_id,
                NEW.remote_node_name, NEW.remote_feed_name, NEW.remote_entry_id
            );
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, OLD.remote_node_name, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        ELSIF TG_OP = 'INSERT' THEN
            PERFORM update_subscription_reference(
                NEW.node_id, NEW.subscription_type, NULL, NULL, NULL,
                NEW.remote_node_name, NEW.remote_feed_name, NEW.remote_entry_id
            );
            IF NEW.subscription_type = 0 THEN
                PERFORM update_subscription_reference(
                    NEW.node_id, 3, NULL, NULL, NULL, NEW.remote_node_name, NULL, NULL
                );
            END IF;
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$;


ALTER FUNCTION public.update_user_subscription_remote_node() OWNER TO moera;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ask_history; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.ask_history (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    subject smallint NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.ask_history OWNER TO moera;

--
-- Name: avatars; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.avatars (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    media_file_id character varying(40) NOT NULL,
    shape character varying(8) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    ordinal integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.avatars OWNER TO moera;

--
-- Name: blocked_by_users; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.blocked_by_users (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    blocked_operation smallint NOT NULL,
    contact_id uuid,
    remote_node_name character varying(63) NOT NULL,
    remote_posting_id character varying(40),
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    reason text DEFAULT ''::text NOT NULL
);


ALTER TABLE public.blocked_by_users OWNER TO moera;

--
-- Name: blocked_instants; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.blocked_instants (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    story_type smallint NOT NULL,
    entry_id uuid,
    created_at timestamp without time zone NOT NULL,
    remote_node_name character varying(63),
    remote_posting_id character varying(40),
    remote_owner_name character varying(63),
    deadline timestamp without time zone
);


ALTER TABLE public.blocked_instants OWNER TO moera;

--
-- Name: blocked_users; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.blocked_users (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    blocked_operation smallint NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    contact_id uuid,
    entry_id uuid,
    entry_node_name character varying(63),
    entry_posting_id character varying(40),
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    reason_src text DEFAULT ''::text NOT NULL,
    reason_src_format smallint DEFAULT 0 NOT NULL,
    reason text DEFAULT ''::text NOT NULL
);


ALTER TABLE public.blocked_users OWNER TO moera;

--
-- Name: contact_upgrades; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.contact_upgrades (
    id bigint NOT NULL,
    node_id uuid NOT NULL,
    upgrade_type smallint NOT NULL,
    remote_node_name character varying(63) NOT NULL
);


ALTER TABLE public.contact_upgrades OWNER TO moera;

--
-- Name: contacts; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.contacts (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_full_name character varying(96),
    closeness_base real DEFAULT 0 NOT NULL,
    closeness real DEFAULT 1 NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    remote_avatar_media_file_id character varying(40),
    remote_avatar_shape character varying(8),
    remote_gender character varying(31),
    feed_subscription_count integer DEFAULT 0 NOT NULL,
    feed_subscriber_count integer DEFAULT 0 NOT NULL,
    friend_count integer DEFAULT 0 NOT NULL,
    friend_of_count integer DEFAULT 0 NOT NULL,
    view_feed_subscription_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    view_feed_subscriber_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    view_friend_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    blocked_user_count integer DEFAULT 0 NOT NULL,
    blocked_user_posting_count integer DEFAULT 0 NOT NULL,
    blocked_by_user_count integer DEFAULT 0 NOT NULL,
    blocked_by_user_posting_count integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.contacts OWNER TO moera;

--
-- Name: domain_upgrades; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.domain_upgrades (
    id bigint NOT NULL,
    upgrade_type smallint NOT NULL,
    node_id uuid NOT NULL
);


ALTER TABLE public.domain_upgrades OWNER TO moera;

--
-- Name: domains; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.domains (
    name character varying(63) NOT NULL,
    node_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.domains OWNER TO moera;

--
-- Name: drafts; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.drafts (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    draft_type smallint NOT NULL,
    receiver_name character varying(63) NOT NULL,
    receiver_posting_id character varying(40),
    receiver_comment_id character varying(40),
    created_at timestamp without time zone NOT NULL,
    edited_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    owner_full_name character varying(96),
    owner_avatar_media_file_id character varying(40),
    owner_avatar_shape character varying(8),
    accepted_reactions_positive character varying(255) NOT NULL,
    accepted_reactions_negative character varying(255) NOT NULL,
    body_src text NOT NULL,
    body_src_format smallint DEFAULT 0 NOT NULL,
    body text NOT NULL,
    body_format character varying(75) DEFAULT 'message'::character varying,
    heading character varying(255) DEFAULT ''::character varying NOT NULL,
    update_important boolean DEFAULT false NOT NULL,
    update_description character varying(128) DEFAULT ''::character varying NOT NULL,
    publish_at timestamp without time zone,
    replied_to_id character varying(40),
    operations text DEFAULT '{}'::text NOT NULL,
    child_operations text DEFAULT '{}'::text NOT NULL
);


ALTER TABLE public.drafts OWNER TO moera;

--
-- Name: entries; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entries (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    entry_type smallint NOT NULL,
    owner_name character varying(63),
    created_at timestamp without time zone NOT NULL,
    current_revision_id uuid,
    deleted_at timestamp without time zone,
    total_revisions integer NOT NULL,
    receiver_name character varying(63),
    accepted_reactions_positive character varying(255) NOT NULL,
    accepted_reactions_negative character varying(255) NOT NULL,
    deadline timestamp without time zone,
    receiver_created_at timestamp without time zone,
    current_receiver_revision_id character varying(40),
    receiver_entry_id character varying(40),
    edited_at timestamp without time zone DEFAULT now() NOT NULL,
    receiver_edited_at timestamp without time zone,
    parent_id uuid,
    total_children integer DEFAULT 0 NOT NULL,
    moment bigint,
    replied_to_id uuid,
    replied_to_name character varying(63),
    replied_to_heading character varying(255),
    replied_to_digest bytea,
    replied_to_revision_id uuid,
    owner_full_name character varying(96),
    receiver_full_name character varying(96),
    replied_to_full_name character varying(96),
    owner_avatar_media_file_id character varying(40),
    owner_avatar_shape character varying(8),
    receiver_avatar_media_file_id character varying(40),
    receiver_avatar_shape character varying(8),
    replied_to_avatar_media_file_id character varying(40),
    replied_to_avatar_shape character varying(8),
    parent_media_id uuid,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_principal character varying(70),
    receiver_deleted_at timestamp without time zone,
    view_comments_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_comments_principal character varying(70),
    add_comment_principal character varying(70) DEFAULT 'signed'::character varying NOT NULL,
    receiver_add_comment_principal character varying(70),
    view_reactions_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_reactions_principal character varying(70),
    view_negative_reactions_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_negative_reactions_principal character varying(70),
    view_reaction_totals_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_reaction_totals_principal character varying(70),
    view_negative_reaction_totals_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_negative_reaction_totals_principal character varying(70),
    add_reaction_principal character varying(70) DEFAULT 'signed'::character varying NOT NULL,
    receiver_add_reaction_principal character varying(70),
    add_negative_reaction_principal character varying(70) DEFAULT 'signed'::character varying NOT NULL,
    receiver_add_negative_reaction_principal character varying(70),
    view_reaction_ratios_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_reaction_ratios_principal character varying(70),
    view_negative_reaction_ratios_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    receiver_view_negative_reaction_ratios_principal character varying(70),
    receiver_edit_principal character varying(70),
    receiver_delete_principal character varying(70),
    parent_view_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_edit_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_delete_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_comments_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_add_comment_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_reactions_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_negative_reactions_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_reaction_totals_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_negative_reaction_totals_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_reaction_ratios_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_view_negative_reaction_ratios_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_add_reaction_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    parent_add_negative_reaction_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    child_operations text DEFAULT '{}'::text NOT NULL,
    parent_override_comment_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    receiver_override_comment_principal character varying(70),
    reaction_operations text DEFAULT '{}'::text NOT NULL,
    child_reaction_operations text DEFAULT '{}'::text NOT NULL,
    parent_override_reaction_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    receiver_override_reaction_principal character varying(70),
    parent_override_comment_reaction_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    receiver_override_comment_reaction_principal character varying(70),
    owner_gender character varying(31),
    receiver_gender character varying(31),
    replied_to_gender character varying(31),
    sheriff_marks text DEFAULT ''::text NOT NULL,
    receiver_sheriff_marks text,
    receiver_sheriffs text,
    sheriff_user_list_referred boolean DEFAULT false NOT NULL
);


ALTER TABLE public.entries OWNER TO moera;

--
-- Name: entry_attachments; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entry_attachments (
    id uuid NOT NULL,
    entry_revision_id uuid,
    media_file_owner_id uuid,
    ordinal integer NOT NULL,
    draft_id uuid,
    embedded boolean DEFAULT true NOT NULL,
    remote_media_id character varying(40),
    remote_media_hash character varying(40),
    remote_media_digest bytea
);


ALTER TABLE public.entry_attachments OWNER TO moera;

--
-- Name: entry_revision_upgrades; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entry_revision_upgrades (
    id bigint NOT NULL,
    upgrade_type smallint NOT NULL,
    entry_revision_id uuid NOT NULL
);


ALTER TABLE public.entry_revision_upgrades OWNER TO moera;

--
-- Name: entry_revisions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entry_revisions (
    entry_id uuid NOT NULL,
    body_src text NOT NULL,
    body text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    signature bytea,
    body_src_format smallint DEFAULT 0 NOT NULL,
    heading character varying(255) DEFAULT ''::character varying NOT NULL,
    body_preview text DEFAULT ''::text NOT NULL,
    id uuid NOT NULL,
    deleted_at timestamp without time zone,
    body_format character varying(75) DEFAULT 'message'::character varying,
    signature_version smallint DEFAULT 0 NOT NULL,
    digest bytea,
    receiver_created_at timestamp without time zone,
    receiver_deleted_at timestamp without time zone,
    receiver_revision_id character varying(40),
    receiver_body_src_hash bytea,
    parent_id uuid,
    deadline timestamp without time zone,
    sane_body text,
    sane_body_preview text,
    update_important boolean DEFAULT false NOT NULL,
    update_description character varying(128) DEFAULT ''::character varying NOT NULL,
    description character varying(255) DEFAULT ''::character varying NOT NULL
);


ALTER TABLE public.entry_revisions OWNER TO moera;

--
-- Name: entry_sources; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entry_sources (
    id uuid NOT NULL,
    entry_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63) NOT NULL,
    remote_posting_id character varying(40) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    remote_full_name character varying(96),
    remote_avatar_media_file_id character varying(40),
    remote_avatar_shape character varying(8)
);


ALTER TABLE public.entry_sources OWNER TO moera;

--
-- Name: friend_groups; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.friend_groups (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    title character varying(63) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL
);


ALTER TABLE public.friend_groups OWNER TO moera;

--
-- Name: friend_ofs; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.friend_ofs (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_group_id character varying(40) NOT NULL,
    remote_group_title character varying(63),
    remote_added_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL,
    contact_id uuid
);


ALTER TABLE public.friend_ofs OWNER TO moera;

--
-- Name: friends; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.friends (
    id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    friend_group_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    node_id uuid NOT NULL,
    contact_id uuid
);


ALTER TABLE public.friends OWNER TO moera;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: moera
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO moera;

--
-- Name: media_file_owners; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.media_file_owners (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    owner_name character varying(63),
    media_file_id character varying(40) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    usage_count integer DEFAULT 0 NOT NULL,
    deadline timestamp without time zone,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    usage_updated_at timestamp without time zone DEFAULT now() NOT NULL,
    permissions_updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.media_file_owners OWNER TO moera;

--
-- Name: media_file_previews; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.media_file_previews (
    id uuid NOT NULL,
    original_media_file_id character varying(40) NOT NULL,
    width integer NOT NULL,
    media_file_id character varying(40)
);


ALTER TABLE public.media_file_previews OWNER TO moera;

--
-- Name: media_files; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.media_files (
    id character varying(40) NOT NULL,
    mime_type character varying(80) NOT NULL,
    size_x integer,
    size_y integer,
    file_size bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    exposed boolean DEFAULT false NOT NULL,
    usage_count integer DEFAULT 0 NOT NULL,
    deadline timestamp without time zone,
    digest bytea,
    orientation smallint DEFAULT 1 NOT NULL
);


ALTER TABLE public.media_files OWNER TO moera;

--
-- Name: option_defaults; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.option_defaults (
    id uuid NOT NULL,
    name character varying(128) NOT NULL,
    value character varying(4096),
    privileged boolean
);


ALTER TABLE public.option_defaults OWNER TO moera;

--
-- Name: options; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.options (
    node_id uuid NOT NULL,
    name character varying(128) NOT NULL,
    value character varying(4096),
    id uuid NOT NULL
);


ALTER TABLE public.options OWNER TO moera;

--
-- Name: own_comments; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.own_comments (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_posting_id character varying(40) NOT NULL,
    remote_comment_id character varying(40) NOT NULL,
    heading character varying(255) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    remote_full_name character varying(96),
    remote_replied_to_name character varying(63),
    remote_replied_to_full_name character varying(96),
    remote_avatar_media_file_id character varying(40),
    remote_avatar_shape character varying(8),
    remote_replied_to_avatar_media_file_id character varying(40),
    remote_replied_to_avatar_shape character varying(8),
    posting_heading character varying(255) DEFAULT ''::character varying NOT NULL
);


ALTER TABLE public.own_comments OWNER TO moera;

--
-- Name: own_postings; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.own_postings (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_full_name character varying(96),
    remote_posting_id character varying(40) NOT NULL,
    heading character varying(255) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    remote_avatar_media_file_id character varying(40),
    remote_avatar_shape character varying(8)
);


ALTER TABLE public.own_postings OWNER TO moera;

--
-- Name: own_reactions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.own_reactions (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_posting_id character varying(40) NOT NULL,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    remote_full_name character varying(96),
    remote_avatar_media_file_id character varying(40),
    remote_avatar_shape character varying(8),
    posting_heading character varying(255) DEFAULT ''::character varying NOT NULL
);


ALTER TABLE public.own_reactions OWNER TO moera;

--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.password_reset_tokens (
    token character varying(10) NOT NULL,
    node_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);


ALTER TABLE public.password_reset_tokens OWNER TO moera;

--
-- Name: pending_notifications; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.pending_notifications (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    notification text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    subscription_created_at timestamp without time zone,
    notification_type smallint NOT NULL
);


ALTER TABLE public.pending_notifications OWNER TO moera;

--
-- Name: picks; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.picks (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    feed_name character varying(63),
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63),
    remote_posting_id character varying(40) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    retry_at timestamp without time zone,
    media_file_owner_id uuid
);


ALTER TABLE public.picks OWNER TO moera;

--
-- Name: public_pages; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.public_pages (
    id bigint NOT NULL,
    node_id uuid NOT NULL,
    after_moment bigint NOT NULL,
    before_moment bigint NOT NULL,
    entry_id uuid
);


ALTER TABLE public.public_pages OWNER TO moera;

--
-- Name: push_clients; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.push_clients (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    client_id character varying(40) NOT NULL,
    last_seen_at timestamp without time zone NOT NULL
);


ALTER TABLE public.push_clients OWNER TO moera;

--
-- Name: push_notifications; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.push_notifications (
    id uuid NOT NULL,
    push_client_id uuid NOT NULL,
    moment bigint NOT NULL,
    content text NOT NULL
);


ALTER TABLE public.push_notifications OWNER TO moera;

--
-- Name: reaction_totals; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.reaction_totals (
    id uuid NOT NULL,
    entry_id uuid,
    entry_revision_id uuid,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    total integer NOT NULL,
    forged boolean DEFAULT false NOT NULL,
    CONSTRAINT reaction_totals_check CHECK (((entry_id IS NOT NULL) OR (entry_revision_id IS NOT NULL)))
);


ALTER TABLE public.reaction_totals OWNER TO moera;

--
-- Name: reactions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.reactions (
    id uuid NOT NULL,
    owner_name character varying(63) NOT NULL,
    entry_revision_id uuid NOT NULL,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    signature_version smallint NOT NULL,
    signature bytea,
    deleted_at timestamp without time zone,
    moment bigint NOT NULL,
    owner_full_name character varying(96),
    owner_avatar_media_file_id character varying(40),
    owner_avatar_shape character varying(8),
    replaced boolean DEFAULT false NOT NULL,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    posting_view_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    posting_delete_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    comment_view_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    comment_delete_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    owner_gender character varying(31)
);


ALTER TABLE public.reactions OWNER TO moera;

--
-- Name: remote_media_cache; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.remote_media_cache (
    id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_media_id character varying(40) NOT NULL,
    digest bytea NOT NULL,
    media_file_id character varying(40),
    deadline timestamp without time zone NOT NULL,
    node_id uuid
);


ALTER TABLE public.remote_media_cache OWNER TO moera;

--
-- Name: remote_user_list_items; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.remote_user_list_items (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    list_node_name character varying(63) NOT NULL,
    list_name character varying(63) NOT NULL,
    node_name character varying(63) NOT NULL,
    absent boolean DEFAULT false NOT NULL,
    cached_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);


ALTER TABLE public.remote_user_list_items OWNER TO moera;

--
-- Name: remote_verifications; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.remote_verifications (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    verification_type smallint NOT NULL,
    node_name character varying(63) NOT NULL,
    posting_id character varying(40) NOT NULL,
    comment_id character varying(40),
    revision_id character varying(40),
    owner_name character varying(63),
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255),
    deadline timestamp without time zone NOT NULL
);


ALTER TABLE public.remote_verifications OWNER TO moera;

--
-- Name: schema_history; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.schema_history OWNER TO moera;

--
-- Name: sheriff_complain_groups; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.sheriff_complain_groups (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_node_full_name character varying(96),
    remote_feed_name character varying(63) NOT NULL,
    remote_posting_owner_name character varying(63),
    remote_posting_owner_full_name character varying(96),
    remote_posting_owner_gender character varying(31),
    remote_posting_heading character varying(255),
    remote_posting_id character varying(40),
    remote_posting_revision_id character varying(40),
    remote_comment_owner_name character varying(63),
    remote_comment_owner_full_name character varying(96),
    remote_comment_owner_gender character varying(31),
    remote_comment_heading character varying(255),
    remote_comment_id character varying(40),
    remote_comment_revision_id character varying(40),
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL,
    status smallint NOT NULL,
    decision_code smallint,
    decision_details text,
    decided_at timestamp without time zone,
    anonymous boolean DEFAULT false NOT NULL
);


ALTER TABLE public.sheriff_complain_groups OWNER TO moera;

--
-- Name: sheriff_complains; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.sheriff_complains (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    owner_name character varying(63),
    owner_full_name character varying(96),
    owner_gender character varying(31),
    group_id uuid NOT NULL,
    reason_code smallint NOT NULL,
    reason_details text,
    created_at timestamp without time zone NOT NULL,
    anonymous_requested boolean DEFAULT false NOT NULL
);


ALTER TABLE public.sheriff_complains OWNER TO moera;

--
-- Name: sheriff_orders; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.sheriff_orders (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    delete boolean DEFAULT false NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63) NOT NULL,
    remote_posting_id character varying(40),
    remote_comment_id character varying(40),
    category smallint NOT NULL,
    reason_code smallint NOT NULL,
    reason_details text,
    created_at timestamp without time zone NOT NULL,
    signature bytea NOT NULL,
    signature_version smallint NOT NULL,
    remote_posting_owner_name character varying(63),
    remote_posting_owner_full_name character varying(96),
    remote_posting_owner_gender character varying(31),
    remote_posting_heading character varying(255),
    remote_posting_revision_id character varying(40),
    remote_comment_owner_name character varying(63),
    remote_comment_owner_full_name character varying(96),
    remote_comment_owner_gender character varying(31),
    remote_comment_heading character varying(255),
    remote_comment_revision_id character varying(40),
    remote_node_full_name character varying(96),
    complain_group_id uuid
);


ALTER TABLE public.sheriff_orders OWNER TO moera;

--
-- Name: sitemap_records; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.sitemap_records (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    sitemap_id uuid NOT NULL,
    entry_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    total_updates integer NOT NULL,
    visible boolean DEFAULT true NOT NULL
);


ALTER TABLE public.sitemap_records OWNER TO moera;

--
-- Name: stories; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.stories (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    node_id uuid NOT NULL,
    feed_name character varying(63) DEFAULT 'timeline'::character varying,
    story_type smallint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL,
    viewed boolean DEFAULT false NOT NULL,
    read boolean DEFAULT false NOT NULL,
    entry_id uuid,
    published_at timestamp without time zone DEFAULT now() NOT NULL,
    pinned boolean DEFAULT false NOT NULL,
    summary text DEFAULT ''::character varying NOT NULL,
    tracking_id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    remote_node_name character varying(63),
    remote_posting_id character varying(40),
    parent_id uuid,
    remote_comment_id character varying(40),
    remote_owner_name character varying(63),
    remote_replied_to_id character varying(40),
    remote_full_name character varying(96),
    remote_owner_full_name character varying(96),
    remote_avatar_media_file_id character varying(40),
    remote_avatar_shape character varying(8),
    remote_owner_avatar_media_file_id character varying(40),
    remote_owner_avatar_shape character varying(8),
    remote_parent_posting_id character varying(40),
    remote_parent_comment_id character varying(40),
    remote_parent_media_id character varying(40),
    remote_posting_node_name character varying(63),
    remote_posting_full_name character varying(96),
    remote_posting_avatar_media_file_id character varying(40),
    remote_posting_avatar_shape character varying(8),
    satisfied boolean DEFAULT false NOT NULL
);


ALTER TABLE public.stories OWNER TO moera;

--
-- Name: subscribers; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.subscribers (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    feed_name character varying(63),
    entry_id uuid,
    remote_node_name character varying(63) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    admin_view_principal character varying(70) DEFAULT 'unset'::character varying NOT NULL,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    contact_id uuid
);


ALTER TABLE public.subscribers OWNER TO moera;

--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.subscriptions (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    remote_subscriber_id character varying(40),
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63),
    remote_entry_id character varying(40),
    created_at timestamp without time zone NOT NULL,
    status smallint DEFAULT 0 NOT NULL,
    retry_at timestamp without time zone,
    usage_count integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.subscriptions OWNER TO moera;

--
-- Name: tokens; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.tokens (
    token character varying(45) NOT NULL,
    name character varying(127),
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    node_id uuid NOT NULL,
    auth_category bigint DEFAULT 0 NOT NULL,
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    ip inet,
    plugin_name character varying(48),
    last_used_at timestamp without time zone,
    last_used_browser character varying(32),
    last_used_ip inet
);


ALTER TABLE public.tokens OWNER TO moera;

--
-- Name: user_list_items; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.user_list_items (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    list_name character varying(63) NOT NULL,
    node_name character varying(63) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL
);


ALTER TABLE public.user_list_items OWNER TO moera;

--
-- Name: user_subscriptions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.user_subscriptions (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    feed_name character varying(63),
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63),
    remote_entry_id character varying(40),
    created_at timestamp without time zone NOT NULL,
    reason smallint DEFAULT 0 NOT NULL,
    view_principal character varying(70) DEFAULT 'public'::character varying NOT NULL,
    contact_id uuid
);


ALTER TABLE public.user_subscriptions OWNER TO moera;

--
-- Name: ask_history ask_history_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.ask_history
    ADD CONSTRAINT ask_history_pkey PRIMARY KEY (id);


--
-- Name: avatars avatars_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.avatars
    ADD CONSTRAINT avatars_pkey PRIMARY KEY (id);


--
-- Name: blocked_by_users blocked_by_users_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_by_users
    ADD CONSTRAINT blocked_by_users_pkey PRIMARY KEY (id);


--
-- Name: blocked_instants blocked_instants_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_instants
    ADD CONSTRAINT blocked_instants_pkey PRIMARY KEY (id);


--
-- Name: blocked_users blocked_users_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_users
    ADD CONSTRAINT blocked_users_pkey PRIMARY KEY (id);


--
-- Name: contact_upgrades contact_upgrades_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.contact_upgrades
    ADD CONSTRAINT contact_upgrades_pkey PRIMARY KEY (id);


--
-- Name: contacts contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_pkey PRIMARY KEY (id);


--
-- Name: domain_upgrades domain_upgrades_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.domain_upgrades
    ADD CONSTRAINT domain_upgrades_pkey PRIMARY KEY (id);


--
-- Name: domains domains_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.domains
    ADD CONSTRAINT domains_pkey PRIMARY KEY (name);


--
-- Name: drafts drafts_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.drafts
    ADD CONSTRAINT drafts_pkey PRIMARY KEY (id);


--
-- Name: entries entries_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_pkey PRIMARY KEY (id);


--
-- Name: entry_attachments entry_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_attachments
    ADD CONSTRAINT entry_attachments_pkey PRIMARY KEY (id);


--
-- Name: entry_revision_upgrades entry_revision_upgrades_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revision_upgrades
    ADD CONSTRAINT entry_revision_upgrades_pkey PRIMARY KEY (id);


--
-- Name: entry_revisions entry_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revisions
    ADD CONSTRAINT entry_revisions_pkey PRIMARY KEY (id);


--
-- Name: entry_sources entry_sources_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_sources
    ADD CONSTRAINT entry_sources_pkey PRIMARY KEY (id);


--
-- Name: friend_groups friend_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.friend_groups
    ADD CONSTRAINT friend_groups_pkey PRIMARY KEY (id);


--
-- Name: friend_ofs friend_ofs_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.friend_ofs
    ADD CONSTRAINT friend_ofs_pkey PRIMARY KEY (id);


--
-- Name: friends friends_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.friends
    ADD CONSTRAINT friends_pkey PRIMARY KEY (id);


--
-- Name: media_file_owners media_file_owners_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.media_file_owners
    ADD CONSTRAINT media_file_owners_pkey PRIMARY KEY (id);


--
-- Name: media_file_previews media_file_previews_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.media_file_previews
    ADD CONSTRAINT media_file_previews_pkey PRIMARY KEY (id);


--
-- Name: media_files media_files_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.media_files
    ADD CONSTRAINT media_files_pkey PRIMARY KEY (id);


--
-- Name: option_defaults option_defaults_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.option_defaults
    ADD CONSTRAINT option_defaults_pkey PRIMARY KEY (id);


--
-- Name: options options_node_id_name_key; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT options_node_id_name_key UNIQUE (node_id, name);


--
-- Name: options options_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT options_pkey PRIMARY KEY (id);


--
-- Name: own_comments own_comments_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_comments
    ADD CONSTRAINT own_comments_pkey PRIMARY KEY (id);


--
-- Name: own_postings own_postings_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_postings
    ADD CONSTRAINT own_postings_pkey PRIMARY KEY (id);


--
-- Name: own_reactions own_reactions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_reactions
    ADD CONSTRAINT own_reactions_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (token);


--
-- Name: pending_notifications pending_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.pending_notifications
    ADD CONSTRAINT pending_notifications_pkey PRIMARY KEY (id);


--
-- Name: picks picks_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.picks
    ADD CONSTRAINT picks_pkey PRIMARY KEY (id);


--
-- Name: public_pages public_pages_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.public_pages
    ADD CONSTRAINT public_pages_pkey PRIMARY KEY (id);


--
-- Name: push_clients push_clients_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.push_clients
    ADD CONSTRAINT push_clients_pkey PRIMARY KEY (id);


--
-- Name: push_notifications push_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.push_notifications
    ADD CONSTRAINT push_notifications_pkey PRIMARY KEY (id);


--
-- Name: reaction_totals reaction_totals_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reaction_totals
    ADD CONSTRAINT reaction_totals_pkey PRIMARY KEY (id);


--
-- Name: reactions reactions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reactions
    ADD CONSTRAINT reactions_pkey PRIMARY KEY (id);


--
-- Name: remote_media_cache remote_media_cache_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.remote_media_cache
    ADD CONSTRAINT remote_media_cache_pkey PRIMARY KEY (id);


--
-- Name: remote_user_list_items remote_user_list_items_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.remote_user_list_items
    ADD CONSTRAINT remote_user_list_items_pkey PRIMARY KEY (id);


--
-- Name: remote_verifications remote_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.remote_verifications
    ADD CONSTRAINT remote_verifications_pkey PRIMARY KEY (id);


--
-- Name: schema_history schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.schema_history
    ADD CONSTRAINT schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: sheriff_complain_groups sheriff_complain_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sheriff_complain_groups
    ADD CONSTRAINT sheriff_complain_groups_pkey PRIMARY KEY (id);


--
-- Name: sheriff_complains sheriff_complains_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sheriff_complains
    ADD CONSTRAINT sheriff_complains_pkey PRIMARY KEY (id);


--
-- Name: sheriff_orders sheriff_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sheriff_orders
    ADD CONSTRAINT sheriff_orders_pkey PRIMARY KEY (id);


--
-- Name: sitemap_records sitemap_records_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sitemap_records
    ADD CONSTRAINT sitemap_records_pkey PRIMARY KEY (id);


--
-- Name: stories stories_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_pkey PRIMARY KEY (id);


--
-- Name: subscribers subscribers_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscribers
    ADD CONSTRAINT subscribers_pkey PRIMARY KEY (id);


--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


--
-- Name: tokens tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.tokens
    ADD CONSTRAINT tokens_pkey PRIMARY KEY (id);


--
-- Name: user_list_items user_list_items_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.user_list_items
    ADD CONSTRAINT user_list_items_pkey PRIMARY KEY (id);


--
-- Name: user_subscriptions user_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.user_subscriptions
    ADD CONSTRAINT user_subscriptions_pkey PRIMARY KEY (id);


--
-- Name: ask_history_node_id_remote_node_name_subject_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX ask_history_node_id_remote_node_name_subject_idx ON public.ask_history USING btree (node_id, remote_node_name, subject);


--
-- Name: avatars_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX avatars_media_file_id_idx ON public.avatars USING btree (media_file_id);


--
-- Name: avatars_node_id_ordinal_created_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX avatars_node_id_ordinal_created_at_idx ON public.avatars USING btree (node_id, ordinal, created_at);


--
-- Name: blocked_by_users_contact_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_by_users_contact_id_idx ON public.blocked_by_users USING btree (contact_id);


--
-- Name: blocked_by_users_node_id_remote_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_by_users_node_id_remote_idx ON public.blocked_by_users USING btree (node_id, remote_node_name, remote_posting_id);


--
-- Name: blocked_instants_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_instants_deadline_idx ON public.blocked_instants USING btree (deadline);


--
-- Name: blocked_instants_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_instants_entry_id_idx ON public.blocked_instants USING btree (entry_id);


--
-- Name: blocked_instants_node_id_type_owner_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_instants_node_id_type_owner_idx ON public.blocked_instants USING btree (node_id, story_type, remote_owner_name);


--
-- Name: blocked_instants_node_id_type_remote_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_instants_node_id_type_remote_idx ON public.blocked_instants USING btree (node_id, story_type, remote_node_name, remote_posting_id);


--
-- Name: blocked_users_contact_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_users_contact_id_idx ON public.blocked_users USING btree (contact_id);


--
-- Name: blocked_users_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_users_deadline_idx ON public.blocked_users USING btree (deadline);


--
-- Name: blocked_users_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_users_entry_id_idx ON public.blocked_users USING btree (entry_id);


--
-- Name: blocked_users_node_id_operation_node_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_users_node_id_operation_node_idx ON public.blocked_users USING btree (node_id, blocked_operation, remote_node_name);


--
-- Name: blocked_users_node_id_remote_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX blocked_users_node_id_remote_idx ON public.blocked_users USING btree (node_id, entry_node_name, entry_posting_id);


--
-- Name: comments_slice_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX comments_slice_idx ON public.entries USING btree (node_id, parent_id, moment, view_principal, owner_name) WHERE (deleted_at IS NULL);


--
-- Name: contact_upgrades_upgrade_type_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX contact_upgrades_upgrade_type_node_id_idx ON public.contact_upgrades USING btree (upgrade_type, node_id);


--
-- Name: contacts_node_id_closeness_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX contacts_node_id_closeness_idx ON public.contacts USING btree (node_id, closeness);


--
-- Name: contacts_node_id_remote_node_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX contacts_node_id_remote_node_name_idx ON public.contacts USING btree (node_id, remote_node_name);


--
-- Name: contacts_remote_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX contacts_remote_avatar_media_file_id_idx ON public.contacts USING btree (remote_avatar_media_file_id);


--
-- Name: contacts_updated_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX contacts_updated_at_idx ON public.contacts USING btree (updated_at);


--
-- Name: domain_upgrades_upgrade_type_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX domain_upgrades_upgrade_type_idx ON public.domain_upgrades USING btree (upgrade_type);


--
-- Name: drafts_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX drafts_deadline_idx ON public.drafts USING btree (deadline);


--
-- Name: drafts_node_id_draft_type_attributes_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX drafts_node_id_draft_type_attributes_idx ON public.drafts USING btree (node_id, draft_type, receiver_name, receiver_posting_id, receiver_comment_id, edited_at);


--
-- Name: drafts_owner_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX drafts_owner_avatar_media_file_id_idx ON public.drafts USING btree (owner_avatar_media_file_id);


--
-- Name: entries_current_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_current_revision_id_idx ON public.entries USING btree (current_revision_id);


--
-- Name: entries_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_deadline_idx ON public.entries USING btree (deadline);


--
-- Name: entries_entry_type_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_entry_type_idx ON public.entries USING btree (entry_type);


--
-- Name: entries_node_id_deleted_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_node_id_deleted_at_idx ON public.entries USING btree (node_id, deleted_at);


--
-- Name: entries_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_node_id_idx ON public.entries USING btree (node_id);


--
-- Name: entries_node_id_owner_name_replied_to_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_node_id_owner_name_replied_to_name_idx ON public.entries USING btree (node_id, owner_name, replied_to_name);


--
-- Name: entries_node_id_receiver_name_receiver_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX entries_node_id_receiver_name_receiver_entry_id_idx ON public.entries USING btree (node_id, receiver_name, receiver_entry_id);


--
-- Name: entries_owner_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_owner_avatar_media_file_id_idx ON public.entries USING btree (owner_avatar_media_file_id);


--
-- Name: entries_parent_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_parent_id_idx ON public.entries USING btree (parent_id);


--
-- Name: entries_parent_id_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_parent_id_moment_idx ON public.entries USING btree (parent_id, moment);


--
-- Name: entries_parent_media_id_not_null_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX entries_parent_media_id_not_null_idx ON public.entries USING btree (parent_media_id, receiver_name) WHERE (receiver_name IS NOT NULL);


--
-- Name: entries_parent_media_id_null_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX entries_parent_media_id_null_idx ON public.entries USING btree (parent_media_id) WHERE (receiver_name IS NULL);


--
-- Name: entries_parent_media_id_receiver_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_parent_media_id_receiver_name_idx ON public.entries USING btree (parent_media_id, receiver_name);


--
-- Name: entries_receiver_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_receiver_avatar_media_file_id_idx ON public.entries USING btree (receiver_avatar_media_file_id);


--
-- Name: entries_replied_to_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_replied_to_avatar_media_file_id_idx ON public.entries USING btree (replied_to_avatar_media_file_id);


--
-- Name: entries_replied_to_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_replied_to_id_idx ON public.entries USING btree (replied_to_id);


--
-- Name: entries_replied_to_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_replied_to_revision_id_idx ON public.entries USING btree (replied_to_revision_id);


--
-- Name: entry_attachments_draft_id_ordinal_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_attachments_draft_id_ordinal_idx ON public.entry_attachments USING btree (draft_id, ordinal);


--
-- Name: entry_attachments_entry_revision_id_ordinal_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_attachments_entry_revision_id_ordinal_idx ON public.entry_attachments USING btree (entry_revision_id, ordinal);


--
-- Name: entry_attachments_media_file_owner_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_attachments_media_file_owner_id_idx ON public.entry_attachments USING btree (media_file_owner_id);


--
-- Name: entry_revision_upgrades_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revision_upgrades_entry_revision_id_idx ON public.entry_revision_upgrades USING btree (entry_revision_id);


--
-- Name: entry_revisions_created_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revisions_created_at_idx ON public.entry_revisions USING btree (created_at);


--
-- Name: entry_revisions_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revisions_deadline_idx ON public.entry_revisions USING btree (deadline);


--
-- Name: entry_revisions_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revisions_entry_id_idx ON public.entry_revisions USING btree (entry_id);


--
-- Name: entry_revisions_parent_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revisions_parent_id_idx ON public.entry_revisions USING btree (parent_id);


--
-- Name: entry_sources_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_sources_entry_id_idx ON public.entry_sources USING btree (entry_id);


--
-- Name: entry_sources_remote_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_sources_remote_avatar_media_file_id_idx ON public.entry_sources USING btree (remote_avatar_media_file_id);


--
-- Name: friend_groups_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX friend_groups_node_id_idx ON public.friend_groups USING btree (node_id);


--
-- Name: friend_ofs_contact_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX friend_ofs_contact_id_idx ON public.friend_ofs USING btree (contact_id);


--
-- Name: friend_ofs_node_id_remote_node_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX friend_ofs_node_id_remote_node_name_idx ON public.friend_ofs USING btree (node_id, remote_node_name);


--
-- Name: friends_contact_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX friends_contact_id_idx ON public.friends USING btree (contact_id);


--
-- Name: friends_friend_group_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX friends_friend_group_id_idx ON public.friends USING btree (friend_group_id);


--
-- Name: friends_node_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX friends_node_name_idx ON public.friends USING btree (remote_node_name);


--
-- Name: media_file_owners_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_file_owners_deadline_idx ON public.media_file_owners USING btree (deadline);


--
-- Name: media_file_owners_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_file_owners_media_file_id_idx ON public.media_file_owners USING btree (media_file_id);


--
-- Name: media_file_owners_node_id_owner_name_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_file_owners_node_id_owner_name_media_file_id_idx ON public.media_file_owners USING btree (node_id, owner_name, media_file_id);


--
-- Name: media_file_previews_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_file_previews_media_file_id_idx ON public.media_file_previews USING btree (media_file_id);


--
-- Name: media_file_previews_original_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_file_previews_original_media_file_id_idx ON public.media_file_previews USING btree (original_media_file_id);


--
-- Name: media_files_created_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_files_created_at_idx ON public.media_files USING btree (created_at);


--
-- Name: media_files_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX media_files_deadline_idx ON public.media_files USING btree (deadline);


--
-- Name: option_defaults_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX option_defaults_name_idx ON public.option_defaults USING btree (name);


--
-- Name: options_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX options_name_idx ON public.options USING btree (name);


--
-- Name: options_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX options_node_id_idx ON public.options USING btree (node_id);


--
-- Name: own_comments_node_id_remote_node_name_remote_posting_id_rem_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX own_comments_node_id_remote_node_name_remote_posting_id_rem_idx ON public.own_comments USING btree (node_id, remote_node_name, remote_posting_id, remote_comment_id);


--
-- Name: own_comments_remote_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX own_comments_remote_avatar_media_file_id_idx ON public.own_comments USING btree (remote_avatar_media_file_id);


--
-- Name: own_comments_remote_replied_to_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX own_comments_remote_replied_to_avatar_media_file_id_idx ON public.own_comments USING btree (remote_replied_to_avatar_media_file_id);


--
-- Name: own_postings_node_id_remote_node_name_remote_posting_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX own_postings_node_id_remote_node_name_remote_posting_id_idx ON public.own_postings USING btree (node_id, remote_node_name, remote_posting_id);


--
-- Name: own_postings_remote_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX own_postings_remote_avatar_media_file_id_idx ON public.own_postings USING btree (remote_avatar_media_file_id);


--
-- Name: own_reactions_node_id_remote_node_name_remote_posting_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX own_reactions_node_id_remote_node_name_remote_posting_id_idx ON public.own_reactions USING btree (node_id, remote_node_name, remote_posting_id);


--
-- Name: own_reactions_remote_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX own_reactions_remote_avatar_media_file_id_idx ON public.own_reactions USING btree (remote_avatar_media_file_id);


--
-- Name: password_reset_tokens_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX password_reset_tokens_deadline_idx ON public.password_reset_tokens USING btree (deadline);


--
-- Name: password_reset_tokens_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX password_reset_tokens_node_id_idx ON public.password_reset_tokens USING btree (node_id);


--
-- Name: pending_notifications_created_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX pending_notifications_created_at_idx ON public.pending_notifications USING btree (created_at);


--
-- Name: pending_notifications_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX pending_notifications_node_id_idx ON public.pending_notifications USING btree (node_id);


--
-- Name: picks_media_file_owner_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX picks_media_file_owner_id_idx ON public.picks USING btree (media_file_owner_id);


--
-- Name: picks_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX picks_node_id_idx ON public.picks USING btree (node_id);


--
-- Name: public_pages_node_id_entry_id_after_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX public_pages_node_id_entry_id_after_moment_idx ON public.public_pages USING btree (node_id, entry_id, after_moment);


--
-- Name: public_pages_node_id_entry_id_before_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX public_pages_node_id_entry_id_before_moment_idx ON public.public_pages USING btree (node_id, entry_id, before_moment);


--
-- Name: public_pages_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX public_pages_node_id_idx ON public.public_pages USING btree (node_id);


--
-- Name: push_clients_node_id_client_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX push_clients_node_id_client_id_idx ON public.push_clients USING btree (node_id, client_id);


--
-- Name: push_clients_node_id_last_seen_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX push_clients_node_id_last_seen_at_idx ON public.push_clients USING btree (node_id, last_seen_at);


--
-- Name: push_notifications_moment_push_client_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX push_notifications_moment_push_client_id_idx ON public.push_notifications USING btree (moment, push_client_id);


--
-- Name: push_notifications_push_client_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX push_notifications_push_client_id_idx ON public.push_notifications USING btree (push_client_id);


--
-- Name: reaction_totals_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reaction_totals_entry_id_idx ON public.reaction_totals USING btree (entry_id);


--
-- Name: reaction_totals_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reaction_totals_entry_revision_id_idx ON public.reaction_totals USING btree (entry_revision_id);


--
-- Name: reactions_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_deadline_idx ON public.reactions USING btree (deadline);


--
-- Name: reactions_entry_revision_id_deleted_at_owner_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_entry_revision_id_deleted_at_owner_name_idx ON public.reactions USING btree (entry_revision_id, deleted_at, owner_name);


--
-- Name: reactions_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_entry_revision_id_idx ON public.reactions USING btree (entry_revision_id);


--
-- Name: reactions_entry_revision_id_negative_emoji_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_entry_revision_id_negative_emoji_idx ON public.reactions USING btree (entry_revision_id, negative, emoji);


--
-- Name: reactions_entry_revision_id_null_owner_name_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX reactions_entry_revision_id_null_owner_name_id_idx ON public.reactions USING btree (entry_revision_id, owner_name) WHERE (deleted_at IS NULL);


--
-- Name: reactions_moment_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_moment_entry_revision_id_idx ON public.reactions USING btree (moment, entry_revision_id);


--
-- Name: reactions_owner_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_owner_avatar_media_file_id_idx ON public.reactions USING btree (owner_avatar_media_file_id);


--
-- Name: remote_media_cache_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_media_cache_deadline_idx ON public.remote_media_cache USING btree (deadline);


--
-- Name: remote_media_cache_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_media_cache_media_file_id_idx ON public.remote_media_cache USING btree (media_file_id);


--
-- Name: remote_media_cache_node_id_remote_node_name_remote_media_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX remote_media_cache_node_id_remote_node_name_remote_media_id_idx ON public.remote_media_cache USING btree (node_id, remote_node_name, remote_media_id);


--
-- Name: remote_media_cache_null_remote_node_name_remote_media_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX remote_media_cache_null_remote_node_name_remote_media_id_idx ON public.remote_media_cache USING btree (remote_node_name, remote_media_id) WHERE (node_id IS NULL);


--
-- Name: remote_user_list_items_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_user_list_items_deadline_idx ON public.remote_user_list_items USING btree (deadline);


--
-- Name: remote_user_list_items_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX remote_user_list_items_name_idx ON public.remote_user_list_items USING btree (node_id, list_node_name, list_name, node_name);


--
-- Name: remote_verifications_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_verifications_deadline_idx ON public.remote_verifications USING btree (deadline);


--
-- Name: remote_verifications_node_id_verification_type_node_name_p_idx1; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_verifications_node_id_verification_type_node_name_p_idx1 ON public.remote_verifications USING btree (node_id, verification_type, node_name, posting_id, owner_name);


--
-- Name: remote_verifications_node_id_verification_type_node_name_po_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_verifications_node_id_verification_type_node_name_po_idx ON public.remote_verifications USING btree (node_id, verification_type, node_name, posting_id, revision_id);


--
-- Name: schema_history_s_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX schema_history_s_idx ON public.schema_history USING btree (success);


--
-- Name: sheriff_complain_groups_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX sheriff_complain_groups_moment_idx ON public.sheriff_complain_groups USING btree (moment);


--
-- Name: sheriff_complain_groups_target_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX sheriff_complain_groups_target_idx ON public.sheriff_complain_groups USING btree (node_id, remote_node_name, remote_feed_name, COALESCE(remote_posting_id, ''::character varying), COALESCE(remote_comment_id, ''::character varying));


--
-- Name: sheriff_complains_group_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX sheriff_complains_group_idx ON public.sheriff_complains USING btree (group_id);


--
-- Name: sheriff_orders_complain_group_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX sheriff_orders_complain_group_idx ON public.sheriff_complain_groups USING btree (id);


--
-- Name: sheriff_orders_target_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX sheriff_orders_target_idx ON public.sheriff_orders USING btree (node_id, remote_node_name, remote_feed_name, remote_posting_id, remote_comment_id);


--
-- Name: sitemap_records_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX sitemap_records_entry_id_idx ON public.sitemap_records USING btree (entry_id);


--
-- Name: sitemap_records_node_id_sitemap_id_modified_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX sitemap_records_node_id_sitemap_id_modified_at_idx ON public.sitemap_records USING btree (node_id, sitemap_id, modified_at);


--
-- Name: stories_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_entry_id_idx ON public.stories USING btree (entry_id);


--
-- Name: stories_node_id_feed_name_created_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_created_at_idx ON public.stories USING btree (node_id, feed_name, created_at);


--
-- Name: stories_node_id_feed_name_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_moment_idx ON public.stories USING btree (node_id, feed_name, moment);


--
-- Name: stories_node_id_feed_name_read_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_read_idx ON public.stories USING btree (node_id, feed_name, read);


--
-- Name: stories_node_id_feed_name_viewed_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_viewed_idx ON public.stories USING btree (node_id, feed_name, viewed);


--
-- Name: stories_node_id_remote_node_name_remote_posting_id_remote_c_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_remote_node_name_remote_posting_id_remote_c_idx ON public.stories USING btree (node_id, remote_node_name, remote_posting_id, remote_comment_id);


--
-- Name: stories_node_id_remote_node_name_remote_posting_id_remote_r_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_remote_node_name_remote_posting_id_remote_r_idx ON public.stories USING btree (node_id, remote_node_name, remote_posting_id, remote_replied_to_id);


--
-- Name: stories_parent_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_parent_id_idx ON public.stories USING btree (parent_id);


--
-- Name: stories_remote_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_remote_avatar_media_file_id_idx ON public.stories USING btree (remote_avatar_media_file_id);


--
-- Name: stories_remote_owner_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_remote_owner_avatar_media_file_id_idx ON public.stories USING btree (remote_owner_avatar_media_file_id);


--
-- Name: stories_remote_posting_avatar_media_file_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_remote_posting_avatar_media_file_id_idx ON public.stories USING btree (remote_posting_avatar_media_file_id);


--
-- Name: stories_tracking_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX stories_tracking_id_idx ON public.stories USING btree (tracking_id);


--
-- Name: subscribers_contact_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_contact_id_idx ON public.subscribers USING btree (contact_id);


--
-- Name: subscribers_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_entry_id_idx ON public.subscribers USING btree (entry_id);


--
-- Name: subscribers_node_id_remote_node_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_node_id_remote_node_name_idx ON public.subscribers USING btree (node_id, remote_node_name);


--
-- Name: subscribers_node_id_type_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_node_id_type_entry_id_idx ON public.subscribers USING btree (node_id, subscription_type, entry_id);


--
-- Name: subscribers_node_id_type_feed_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_node_id_type_feed_name_idx ON public.subscribers USING btree (node_id, subscription_type, feed_name);


--
-- Name: subscriptions_node_id_type_remote_node_active_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX subscriptions_node_id_type_remote_node_active_idx ON public.subscriptions USING btree (node_id, subscription_type, remote_node_name, COALESCE(remote_feed_name, ''::character varying), COALESCE(remote_entry_id, ''::character varying)) WHERE (usage_count > 0);


--
-- Name: subscriptions_node_id_type_remote_node_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscriptions_node_id_type_remote_node_idx ON public.subscriptions USING btree (node_id, subscription_type, remote_node_name, remote_subscriber_id);


--
-- Name: subscriptions_status_usage_count_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscriptions_status_usage_count_idx ON public.subscriptions USING btree (status, usage_count);


--
-- Name: tokens_token_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX tokens_token_idx ON public.tokens USING btree (token);


--
-- Name: user_list_items_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX user_list_items_moment_idx ON public.user_list_items USING btree (node_id, list_name, moment);


--
-- Name: user_list_items_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX user_list_items_name_idx ON public.user_list_items USING btree (node_id, list_name, node_name);


--
-- Name: user_subscriptions_contact_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX user_subscriptions_contact_id_idx ON public.user_subscriptions USING btree (contact_id);


--
-- Name: media_file_owners update_deadline; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_deadline BEFORE INSERT OR UPDATE OF usage_count, deadline ON public.media_file_owners FOR EACH ROW EXECUTE FUNCTION public.update_media_file_deadline();


--
-- Name: media_files update_deadline; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_deadline BEFORE INSERT OR UPDATE OF usage_count, deadline ON public.media_files FOR EACH ROW EXECUTE FUNCTION public.update_media_file_deadline();


--
-- Name: avatars update_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_media_file_id AFTER INSERT OR DELETE OR UPDATE OF media_file_id ON public.avatars FOR EACH ROW EXECUTE FUNCTION public.update_entity_media_file_id();


--
-- Name: media_file_owners update_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_media_file_id AFTER INSERT OR DELETE OR UPDATE OF media_file_id ON public.media_file_owners FOR EACH ROW EXECUTE FUNCTION public.update_entity_media_file_id();


--
-- Name: media_file_previews update_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_media_file_id AFTER INSERT OR DELETE OR UPDATE OF media_file_id, original_media_file_id ON public.media_file_previews FOR EACH ROW EXECUTE FUNCTION public.update_media_file_preview_media_file_id();


--
-- Name: entry_attachments update_media_file_owner_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_media_file_owner_id AFTER INSERT OR DELETE OR UPDATE OF media_file_owner_id ON public.entry_attachments FOR EACH ROW EXECUTE FUNCTION public.update_entry_attachments_media_file_owner_id();


--
-- Name: entries update_media_file_owner_usage; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_media_file_owner_usage AFTER UPDATE OF current_revision_id, deleted_at, view_principal ON public.entries FOR EACH ROW EXECUTE FUNCTION public.update_entry_media_file_owner_usage();


--
-- Name: drafts update_owner_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_owner_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF owner_avatar_media_file_id ON public.drafts FOR EACH ROW EXECUTE FUNCTION public.update_entity_owner_avatar_media_file_id();


--
-- Name: entries update_owner_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_owner_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF owner_avatar_media_file_id ON public.entries FOR EACH ROW EXECUTE FUNCTION public.update_entity_owner_avatar_media_file_id();


--
-- Name: reactions update_owner_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_owner_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF owner_avatar_media_file_id ON public.reactions FOR EACH ROW EXECUTE FUNCTION public.update_entity_owner_avatar_media_file_id();


--
-- Name: entries update_receiver_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_receiver_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF receiver_avatar_media_file_id ON public.entries FOR EACH ROW EXECUTE FUNCTION public.update_entity_receiver_avatar_media_file_id();


--
-- Name: contacts update_remote_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON public.contacts FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_avatar_media_file_id();


--
-- Name: entry_sources update_remote_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON public.entry_sources FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_avatar_media_file_id();


--
-- Name: own_comments update_remote_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON public.own_comments FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_avatar_media_file_id();


--
-- Name: own_postings update_remote_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON public.own_postings FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_avatar_media_file_id();


--
-- Name: own_reactions update_remote_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON public.own_reactions FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_avatar_media_file_id();


--
-- Name: stories update_remote_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_avatar_media_file_id ON public.stories FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_avatar_media_file_id();


--
-- Name: blocked_by_users update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name ON public.blocked_by_users FOR EACH ROW EXECUTE FUNCTION public.update_blocked_by_user_remote_node();


--
-- Name: blocked_users update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name ON public.blocked_users FOR EACH ROW EXECUTE FUNCTION public.update_blocked_user_remote_node();


--
-- Name: contacts update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name ON public.contacts FOR EACH ROW EXECUTE FUNCTION public.update_contact_remote_node();


--
-- Name: entries update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, receiver_name, receiver_entry_id, deleted_at ON public.entries FOR EACH ROW EXECUTE FUNCTION public.update_entry_remote_node();


--
-- Name: friend_ofs update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name ON public.friend_ofs FOR EACH ROW EXECUTE FUNCTION public.update_friend_of_remote_node();


--
-- Name: friends update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name ON public.friends FOR EACH ROW EXECUTE FUNCTION public.update_friend_remote_node();


--
-- Name: subscribers update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE OF node_id, remote_node_name ON public.subscribers FOR EACH ROW EXECUTE FUNCTION public.update_subscriber_remote_node();


--
-- Name: user_subscriptions update_remote_node; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_node AFTER INSERT OR DELETE OR UPDATE ON public.user_subscriptions FOR EACH ROW EXECUTE FUNCTION public.update_user_subscription_remote_node();


--
-- Name: stories update_remote_owner_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_owner_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_owner_avatar_media_file_id ON public.stories FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_owner_avatar_media_file_id();


--
-- Name: stories update_remote_posting_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_posting_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_posting_avatar_media_file_id ON public.stories FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_posting_avatar_media_file_id();


--
-- Name: own_comments update_remote_replied_to_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_remote_replied_to_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF remote_replied_to_avatar_media_file_id ON public.own_comments FOR EACH ROW EXECUTE FUNCTION public.update_entity_remote_replied_to_avatar_media_file_id();


--
-- Name: entries update_replied_to_avatar_media_file_id; Type: TRIGGER; Schema: public; Owner: moera
--

CREATE TRIGGER update_replied_to_avatar_media_file_id AFTER INSERT OR DELETE OR UPDATE OF replied_to_avatar_media_file_id ON public.entries FOR EACH ROW EXECUTE FUNCTION public.update_entity_replied_to_avatar_media_file_id();


--
-- Name: avatars avatars_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.avatars
    ADD CONSTRAINT avatars_media_file_id_fkey FOREIGN KEY (media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: blocked_by_users blocked_by_users_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_by_users
    ADD CONSTRAINT blocked_by_users_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: blocked_instants blocked_instants_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_instants
    ADD CONSTRAINT blocked_instants_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: blocked_users blocked_users_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_users
    ADD CONSTRAINT blocked_users_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: blocked_users blocked_users_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.blocked_users
    ADD CONSTRAINT blocked_users_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: contacts contacts_remote_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_remote_avatar_media_file_id_fkey FOREIGN KEY (remote_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: drafts drafts_owner_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.drafts
    ADD CONSTRAINT drafts_owner_avatar_media_file_id_fkey FOREIGN KEY (owner_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_current_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_current_revision_id_fkey FOREIGN KEY (current_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_owner_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_owner_avatar_media_file_id_fkey FOREIGN KEY (owner_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entries entries_parent_media_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_parent_media_id_fkey FOREIGN KEY (parent_media_id) REFERENCES public.media_file_owners(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entries entries_receiver_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_receiver_avatar_media_file_id_fkey FOREIGN KEY (receiver_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_replied_to_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_replied_to_avatar_media_file_id_fkey FOREIGN KEY (replied_to_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_replied_to_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_replied_to_id_fkey FOREIGN KEY (replied_to_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_replied_to_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_replied_to_revision_id_fkey FOREIGN KEY (replied_to_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entry_attachments entry_attachments_draft_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_attachments
    ADD CONSTRAINT entry_attachments_draft_id_fkey FOREIGN KEY (draft_id) REFERENCES public.drafts(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_attachments entry_attachments_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_attachments
    ADD CONSTRAINT entry_attachments_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_attachments entry_attachments_media_file_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_attachments
    ADD CONSTRAINT entry_attachments_media_file_owner_id_fkey FOREIGN KEY (media_file_owner_id) REFERENCES public.media_file_owners(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_revision_upgrades entry_revision_upgrades_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revision_upgrades
    ADD CONSTRAINT entry_revision_upgrades_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_revisions entry_revisions_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revisions
    ADD CONSTRAINT entry_revisions_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_revisions entry_revisions_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revisions
    ADD CONSTRAINT entry_revisions_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_sources entry_sources_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_sources
    ADD CONSTRAINT entry_sources_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_sources entry_sources_remote_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_sources
    ADD CONSTRAINT entry_sources_remote_avatar_media_file_id_fkey FOREIGN KEY (remote_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: friend_ofs friend_ofs_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.friend_ofs
    ADD CONSTRAINT friend_ofs_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: friends friends_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.friends
    ADD CONSTRAINT friends_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: friends friends_friend_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.friends
    ADD CONSTRAINT friends_friend_group_id_fkey FOREIGN KEY (friend_group_id) REFERENCES public.friend_groups(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: media_file_owners media_file_owners_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.media_file_owners
    ADD CONSTRAINT media_file_owners_media_file_id_fkey FOREIGN KEY (media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: media_file_previews media_file_previews_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.media_file_previews
    ADD CONSTRAINT media_file_previews_media_file_id_fkey FOREIGN KEY (media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: media_file_previews media_file_previews_original_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.media_file_previews
    ADD CONSTRAINT media_file_previews_original_media_file_id_fkey FOREIGN KEY (original_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: own_comments own_comments_remote_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_comments
    ADD CONSTRAINT own_comments_remote_avatar_media_file_id_fkey FOREIGN KEY (remote_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: own_comments own_comments_remote_replied_to_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_comments
    ADD CONSTRAINT own_comments_remote_replied_to_avatar_media_file_id_fkey FOREIGN KEY (remote_replied_to_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: own_postings own_postings_remote_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_postings
    ADD CONSTRAINT own_postings_remote_avatar_media_file_id_fkey FOREIGN KEY (remote_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: own_reactions own_reactions_remote_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.own_reactions
    ADD CONSTRAINT own_reactions_remote_avatar_media_file_id_fkey FOREIGN KEY (remote_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: picks picks_media_file_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.picks
    ADD CONSTRAINT picks_media_file_owner_id_fkey FOREIGN KEY (media_file_owner_id) REFERENCES public.media_file_owners(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: public_pages public_pages_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.public_pages
    ADD CONSTRAINT public_pages_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: push_notifications push_notifications_push_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.push_notifications
    ADD CONSTRAINT push_notifications_push_client_id_fkey FOREIGN KEY (push_client_id) REFERENCES public.push_clients(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reaction_totals reaction_totals_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reaction_totals
    ADD CONSTRAINT reaction_totals_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reaction_totals reaction_totals_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reaction_totals
    ADD CONSTRAINT reaction_totals_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reactions reactions_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reactions
    ADD CONSTRAINT reactions_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reactions reactions_owner_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reactions
    ADD CONSTRAINT reactions_owner_avatar_media_file_id_fkey FOREIGN KEY (owner_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: remote_media_cache remote_media_cache_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.remote_media_cache
    ADD CONSTRAINT remote_media_cache_media_file_id_fkey FOREIGN KEY (media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: sheriff_complains sheriff_complains_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sheriff_complains
    ADD CONSTRAINT sheriff_complains_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.sheriff_complain_groups(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: sheriff_orders sheriff_orders_complain_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sheriff_orders
    ADD CONSTRAINT sheriff_orders_complain_group_id_fkey FOREIGN KEY (complain_group_id) REFERENCES public.sheriff_complain_groups(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: sitemap_records sitemap_records_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.sitemap_records
    ADD CONSTRAINT sitemap_records_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: stories stories_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: stories stories_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.stories(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: stories stories_remote_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_remote_avatar_media_file_id_fkey FOREIGN KEY (remote_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: stories stories_remote_owner_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_remote_owner_avatar_media_file_id_fkey FOREIGN KEY (remote_owner_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: stories stories_remote_posting_avatar_media_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_remote_posting_avatar_media_file_id_fkey FOREIGN KEY (remote_posting_avatar_media_file_id) REFERENCES public.media_files(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: subscribers subscribers_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscribers
    ADD CONSTRAINT subscribers_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: subscribers subscribers_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscribers
    ADD CONSTRAINT subscribers_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_subscriptions user_subscriptions_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.user_subscriptions
    ADD CONSTRAINT user_subscriptions_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

