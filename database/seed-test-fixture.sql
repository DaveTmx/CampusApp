-- Reset fixture for Challenge 1's 20-repetition test: G01 at 14/15,
-- two unassigned contenders ready to race for the last place.
DELETE FROM operations;
DELETE FROM students;

INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
SELECT
  UUID(),
  CONCAT('Seed Student ', n),
  LPAD(n, 9, '0'),
  ELT(1 + (n MOD 3), 'CS', 'IT', 'DS'),
  'G01',
  1
FROM (SELECT ROW_NUMBER() OVER () AS n FROM information_schema.columns LIMIT 14) seq;

INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
VALUES
  ('contender-a-0000-0000-0000-000000000001', 'Contender A', '900000001', 'CS', NULL, 1),
  ('contender-b-0000-0000-0000-000000000002', 'Contender B', '900000002', 'IT', NULL, 1);
