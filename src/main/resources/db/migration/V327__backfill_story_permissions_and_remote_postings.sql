UPDATE stories s
SET view_principal = e.view_principal
FROM entries e
WHERE s.entry_id = e.id
  AND s.feed_name = 'timeline'
  AND s.story_type = 0
  AND e.entry_type = 0
  AND e.receiver_name IS NOT NULL;

UPDATE stories s
SET remote_node_name = e.receiver_name,
    remote_posting_id = e.receiver_entry_id
FROM entries e
WHERE s.entry_id = e.id
  AND s.story_type = 0
  AND e.entry_type = 0
  AND e.receiver_name IS NOT NULL;
