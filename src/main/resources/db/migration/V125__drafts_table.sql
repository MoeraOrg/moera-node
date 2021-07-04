CREATE TABLE drafts (
    id uuid NOT NULL PRIMARY KEY,
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
    reactions_visible boolean DEFAULT true NOT NULL,
    reaction_totals_visible boolean DEFAULT true NOT NULL,
    body_src text NOT NULL,
    body_src_format smallint DEFAULT 0 NOT NULL,
    body text NOT NULL,
    body_format character varying(75) DEFAULT 'message'::character varying,
    heading character varying(255) DEFAULT ''::character varying NOT NULL,
    update_important boolean DEFAULT false NOT NULL,
    update_description character varying(128) DEFAULT ''::character varying NOT NULL
);
CREATE INDEX drafts_node_id_draft_type_attributes_idx
    ON drafts(node_id, draft_type, receiver_name, receiver_posting_id, receiver_comment_id, edited_at);
CREATE INDEX drafts_deadline_idx ON drafts(deadline);
CREATE INDEX drafts_owner_avatar_media_file_id_idx ON drafts(owner_avatar_media_file_id);
ALTER TABLE drafts ADD FOREIGN KEY (owner_avatar_media_file_id) REFERENCES media_files(id)
    ON UPDATE CASCADE ON DELETE SET NULL;
CREATE TRIGGER update_owner_avatar_media_file_id
    AFTER INSERT OR UPDATE OF owner_avatar_media_file_id OR DELETE ON drafts
    FOR EACH ROW EXECUTE PROCEDURE update_entity_owner_avatar_media_file_id();
INSERT INTO drafts(id, node_id, draft_type, receiver_name, created_at, edited_at, deadline, owner_full_name,
    owner_avatar_media_file_id, owner_avatar_shape, accepted_reactions_positive, accepted_reactions_negative,
    reactions_visible, reaction_totals_visible, body_src, body_src_format, body, body_format, heading)
    SELECT
        entries.id,
        entries.node_id,
        0,
        options.value,
        entries.created_at,
        entries.edited_at,
        entries.deadline,
        entries.owner_full_name,
        entries.owner_avatar_media_file_id,
        entries.owner_avatar_shape,
        entries.accepted_reactions_positive,
        entries.accepted_reactions_negative,
        entries.reactions_visible,
        entries.reaction_totals_visible,
        entry_revisions.body_src,
        entry_revisions.body_src_format,
        entry_revisions.body,
        entry_revisions.body_format,
        entry_revisions.heading
    FROM entries
        LEFT JOIN entry_revisions ON entry_revisions.id = entries.draft_revision_id
        LEFT JOIN options ON options.node_id = entries.node_id AND options.name = 'profile.node-name'
    WHERE entries.deleted_at IS NULL AND entries.draft = true;
INSERT INTO drafts(id, node_id, draft_type, receiver_name, receiver_posting_id, created_at, edited_at, deadline,
    owner_full_name, owner_avatar_media_file_id, owner_avatar_shape, accepted_reactions_positive,
    accepted_reactions_negative, reactions_visible, reaction_totals_visible, body_src, body_src_format, body,
    body_format, heading)
    SELECT
        entries.draft_revision_id,
        entries.node_id,
        1,
        options.value,
        entries.id::text,
        entry_revisions.created_at,
        entry_revisions.created_at,
        entry_revisions.deadline,
        entries.owner_full_name,
        entries.owner_avatar_media_file_id,
        entries.owner_avatar_shape,
        entries.accepted_reactions_positive,
        entries.accepted_reactions_negative,
        entries.reactions_visible,
        entries.reaction_totals_visible,
        entry_revisions.body_src,
        entry_revisions.body_src_format,
        entry_revisions.body,
        entry_revisions.body_format,
        entry_revisions.heading
    FROM entries
        LEFT JOIN entry_revisions ON entry_revisions.id = entries.draft_revision_id
        LEFT JOIN options ON options.node_id = entries.node_id AND options.name = 'profile.node-name'
    WHERE entries.deleted_at IS NULL AND entries.draft = false AND entries.draft_revision_id IS NOT NULL;
