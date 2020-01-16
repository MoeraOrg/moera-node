UPDATE entries SET accepted_reactions_positive='+0x1f4a1,+0x1f44d,+0x1f4af,+0x1f60d,+0x1f600,+0x1f926,+0x1f62e,+0x1f622,+0x1f620,+0x1f92e,*' WHERE accepted_reactions_positive IS NULL;
UPDATE entries SET accepted_reactions_negative='+0x1f4a4,+0x1f44e,+0x1f4a9,+0x2694,+0x23f3,+0x1f3a9,+0x1f921,+0x1f61c,+0x1f494,+0x1f47f' WHERE accepted_reactions_negative IS NULL;
DELETE FROM options WHERE name LIKE 'posting.reactions.%.accepted';
ALTER TABLE entries ALTER COLUMN accepted_reactions_positive SET NOT NULL;
ALTER TABLE entries ALTER COLUMN accepted_reactions_negative SET NOT NULL;
