-- Run once, in MySQL Workbench, on your EXISTING database (ict361_lab). Safe to run twice.
-- Result: exactly 14 lab groups G01 to G14, each holding 16 students.
--   * adds any of G01 to G14 that are missing
--   * raises every one of them to 16 places (raising a limit never affects students already placed)
--   * removes groups after G14 (for example G15 to G18 from an earlier trial) but ONLY if nobody is in them
USE ict361_lab;

INSERT IGNORE INTO lab_groups (group_code, capacity) VALUES
  ('G01', 16), ('G02', 16), ('G03', 16), ('G04', 16), ('G05', 16), ('G06', 16), ('G07', 16),
  ('G08', 16), ('G09', 16), ('G10', 16), ('G11', 16), ('G12', 16), ('G13', 16), ('G14', 16);

UPDATE lab_groups SET capacity = 16 WHERE group_code <= 'G14';

DELETE FROM lab_groups
WHERE group_code > 'G14'
  AND group_code NOT IN (SELECT DISTINCT group_code FROM students WHERE group_code IS NOT NULL);

ALTER TABLE lab_groups ALTER COLUMN capacity SET DEFAULT 16;

SELECT COUNT(*) AS groups_total, MIN(group_code) AS first_group, MAX(group_code) AS last_group,
       MIN(capacity) AS min_capacity, MAX(capacity) AS max_capacity FROM lab_groups;
-- expect: 14, G01, G14, 16, 16
-- If last_group is after G14, some students are still in those groups: move them first, then run this again.
