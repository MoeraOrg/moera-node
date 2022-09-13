UPDATE stories
SET remote_posting_avatar_media_file_id = remote_owner_avatar_media_file_id,
    remote_posting_avatar_shape = remote_owner_avatar_shape
WHERE story_type = 12 OR story_type = 13 OR story_type = 24;
