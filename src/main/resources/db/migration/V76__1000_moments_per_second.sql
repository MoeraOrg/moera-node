UPDATE public_pages
SET after_moment = after_moment * 10
WHERE after_moment > -1000000000000 AND after_moment < 1000000000000;
UPDATE public_pages
SET before_moment = before_moment * 10
WHERE before_moment > -1000000000000 AND before_moment < 1000000000000;
UPDATE reactions SET moment = moment * 10;
UPDATE stories SET moment = moment * 10;
