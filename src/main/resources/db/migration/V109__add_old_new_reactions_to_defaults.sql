UPDATE options SET value = concat(value, ',0x1f64f')
WHERE name like '%reactions.positive.available' AND value NOT ILIKE '%0x1f64f%';
UPDATE options SET value = concat(value, ',0x1f60c')
WHERE name like '%reactions.positive.available' AND value NOT ILIKE '%0x1f60c%';
UPDATE options SET value = concat(value, ',0x1f917')
WHERE name like '%reactions.positive.available' AND value NOT ILIKE '%0x1f917%';
UPDATE options SET value = concat(value, ',0x1f525')
WHERE name like '%reactions.positive.available' AND value NOT ILIKE '%0x1f525%';
UPDATE options SET value = replace(value, '0x1f917', '0x1fac2') WHERE name like '%reactions.positive.available';
UPDATE options SET value = replace(value, '0x1f917', '0x1fac2') WHERE name like '%reactions.positive.default';
