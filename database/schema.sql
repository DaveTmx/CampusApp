-- Additions needed for Challenges 1-3. Does not touch `users` (already
-- working in authController.js). Run against the ict361_lab database
-- (matches src/db/pool.js's default DB_NAME).
--
-- NOTE on existing code: groupController.js reads from `lab_groups`
-- (getAllGroups) but writes to `study_groups` (createGroup) -- two
-- different tables. This schema defines `lab_groups` as the ICT361
-- G01-G04 registration groups (capacity 15) used by the challenges below.
-- If `study_groups` is meant to be a separate "campus life" collaboration
-- feature (for GroupChatActivity/GroupsActivity), keep it separate and
-- rename one of the two in groupController.js so they stop colliding on
-- the word "group". Confirm which with whoever wrote groupController.js.

CREATE TABLE IF NOT EXISTS lab_groups (
  group_code  CHAR(3) PRIMARY KEY,         -- G01..G04
  capacity    INT NOT NULL DEFAULT 15
) ENGINE=InnoDB;

INSERT IGNORE INTO lab_groups (group_code, capacity) VALUES
  ('G01', 15), ('G02', 15), ('G03', 15), ('G04', 15);

CREATE TABLE IF NOT EXISTS students (
  student_id      CHAR(36) PRIMARY KEY,
  student_name    VARCHAR(100) NOT NULL,
  student_number  CHAR(9) NOT NULL,
  programme_code  ENUM('CS','IT','DS') NOT NULL,
  group_code      CHAR(3) NULL,
  account_id      INT NULL,                 -- references users.id (student's own login)
  version         INT NOT NULL DEFAULT 1,
  deleted_at      DATETIME NULL,
  UNIQUE KEY uq_student_number (student_number),
  CONSTRAINT fk_student_group FOREIGN KEY (group_code) REFERENCES lab_groups(group_code),
  CONSTRAINT fk_student_account FOREIGN KEY (account_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Powers Challenge 2: one row per client-generated operationId. A retried
-- mutation looks itself up here before doing anything, so it can replay
-- its own result instead of re-running.
CREATE TABLE IF NOT EXISTS operations (
  operation_id  CHAR(36) PRIMARY KEY,
  account_id    INT NULL,
  student_id    CHAR(36) NULL,
  op_type       VARCHAR(20) NOT NULL,
  payload_json  JSON NOT NULL,
  status        ENUM('APPLIED','FAILED') NOT NULL,
  result_json   JSON NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
