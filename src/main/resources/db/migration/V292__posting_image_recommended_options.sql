UPDATE options
SET name = 'media.image.recommended-size'
WHERE name = 'posting.image.recommended-size';

DELETE FROM options WHERE name = 'posting.image.recommended-pixels';
