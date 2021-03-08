UPDATE options SET value = replace(value, '0x1f381', '0x1f49d') WHERE name like '%reactions.positive.available';
UPDATE options SET value = replace(value, '0x1f921', '0x1f643') WHERE name like '%reactions.negative.available';
UPDATE options SET value = concat(value, ',0x1f923,0x1f970') WHERE name like '%reactions.positive.available';
