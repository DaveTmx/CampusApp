-- Reset fixture for Challenge 1's 20-repetition test: G01 at 15/16 (capacity - 1),
-- two unassigned contenders ready to race for the last place.
DELETE FROM operations;
DELETE FROM students;

INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
SELECT
  UUID(),
  CONCAT('Seed Student ', n),
  LPAD(n, 9, '0'),
  ELT(1 + (n MOD 4), 'CS', 'IT', 'DS', 'CYB'),
  'G01',
  1
FROM (SELECT ROW_NUMBER() OVER () AS n FROM information_schema.columns LIMIT 15) seq;

INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
VALUES
  ('a0000000-0000-0000-0000-000000000001', 'Contender A', '900000001', 'CS', NULL, 1),
  ('b0000000-0000-0000-0000-000000000002', 'Contender B', '900000002', 'IT', NULL, 1);
