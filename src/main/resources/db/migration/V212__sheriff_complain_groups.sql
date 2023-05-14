CREATE TABLE sheriff_complain_groups (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    remote_node_name varchar(63) NOT NULL,
    remote_node_full_name varchar(96),
    remote_feed_name varchar(63) NOT NULL,
    remote_posting_owner_name varchar(63),
    remote_posting_owner_full_name varchar(96),
    remote_posting_owner_gender varchar(31),
    remote_posting_heading varchar(255),
    remote_posting_id varchar(40),
    remote_posting_revision_id varchar(40),
    remote_comment_owner_name varchar(63),
    remote_comment_owner_full_name varchar(96),
    remote_comment_owner_gender varchar(31),
    remote_comment_heading varchar(255),
    remote_comment_id varchar(40),
    remote_comment_revision_id varchar(40),
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL,
    status smallint NOT NULL,
    sheriff_decision_id uuid
);
CREATE UNIQUE INDEX sheriff_complain_groups_target_idx
    ON sheriff_complain_groups(node_id, remote_node_name, remote_feed_name, coalesce(remote_posting_id, ''),
                               coalesce(remote_comment_id, ''));
CREATE INDEX sheriff_complain_groups_decision_idx ON sheriff_complain_groups(sheriff_decision_id);
CREATE INDEX sheriff_complain_groups_moment_idx ON sheriff_complain_groups(moment);
ALTER TABLE sheriff_complain_groups ADD FOREIGN KEY (sheriff_decision_id) REFERENCES sheriff_decisions(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

DROP TABLE sheriff_complains;
CREATE TABLE sheriff_complains (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    owner_name varchar(63),
    owner_full_name varchar(96),
    owner_gender varchar(31),
    group_id uuid NOT NULL,
    reason_code smallint NOT NULL,
    reason_details text,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX sheriff_complains_group_idx ON sheriff_complains(group_id);
ALTER TABLE sheriff_complains ADD FOREIGN KEY (group_id) REFERENCES sheriff_complain_groups(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
