-- Campus Companion backend: full schema. Run once:
--   mysql -u root -p < migrations/init.sql
-- Safe to re-run (CREATE TABLE IF NOT EXISTS / INSERT IGNORE).
-- lab_groups, students and operations match database/schema.sql in the zip.

CREATE DATABASE IF NOT EXISTS ict361_lab;
USE ict361_lab;

-- ---------------------------------------------------------------- users
-- Login accounts (authController). `role` is what login puts in the JWT.
-- Lecturers are not self-registered. Register the account, then promote it:
--   UPDATE users SET role = 'lecturer' WHERE email = 'muleba@gmail.com';
CREATE TABLE IF NOT EXISTS users (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  full_name   VARCHAR(100) NOT NULL,
  email       VARCHAR(255) NOT NULL,
  password    VARCHAR(255) NOT NULL,
  role        ENUM('student','lecturer') NOT NULL DEFAULT 'student',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- groups
-- ICT361 registration groups G01-G04, capacity 15.
CREATE TABLE IF NOT EXISTS lab_groups (
  group_code  CHAR(3) PRIMARY KEY,
  capacity    INT NOT NULL DEFAULT 15
) ENGINE=InnoDB;

INSERT IGNORE INTO lab_groups (group_code, capacity) VALUES
  ('G01', 15), ('G02', 15), ('G03', 15), ('G04', 15);

-- Separate "campus life" study groups (groupController.createGroup).
CREATE TABLE IF NOT EXISTS study_groups (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  description  VARCHAR(500) NULL,
  course_code  VARCHAR(20) NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- students
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

-- ---------------------------------------------------------------- operations
-- One row per client-generated operationId (idempotent retries, /api/sync).
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

-- ---------------------------------------------------------------- pending_changes
-- Student requests (change name / group / number) waiting for a lecturer.
CREATE TABLE IF NOT EXISTS pending_changes (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id    CHAR(36) NOT NULL,
  student_name  VARCHAR(100) NOT NULL,       -- snapshot for the lecturer's queue
  field         ENUM('name','group','number') NOT NULL,
  new_value     VARCHAR(100) NOT NULL,
  status        ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at   DATETIME NULL,
  resolved_by   INT NULL,
  KEY idx_pending_status (status),
  CONSTRAINT fk_pending_student FOREIGN KEY (student_id) REFERENCES students(student_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- notices
CREATE TABLE IF NOT EXISTS notices (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(150) NOT NULL,
  body        TEXT NOT NULL,
  category    ENUM('Academic','Admin','Urgent') NOT NULL,
  created_by  INT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Which account has already seen which notice (drives the "new" badge).
CREATE TABLE IF NOT EXISTS notice_reads (
  notice_id   BIGINT NOT NULL,
  account_id  INT NOT NULL,
  read_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notice_id, account_id),
  CONSTRAINT fk_read_notice FOREIGN KEY (notice_id) REFERENCES notices(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- chat
CREATE TABLE IF NOT EXISTS chat_messages (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_code         CHAR(3) NOT NULL,
  sender_account_id  INT NOT NULL,
  sender_name        VARCHAR(100) NOT NULL,
  message_text       VARCHAR(1000) NOT NULL,
  created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_chat_group (group_code, id),
  CONSTRAINT fk_chat_group FOREIGN KEY (group_code) REFERENCES lab_groups(group_code)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- issues
CREATE TABLE IF NOT EXISTS issues (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id   INT NOT NULL,
  category     VARCHAR(80) NOT NULL,
  description  VARCHAR(1000) NOT NULL,
  status       ENUM('Pending','In Progress','Resolved') NOT NULL DEFAULT 'Pending',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_issue_account (account_id),
  KEY idx_issue_status (status),
  CONSTRAINT fk_issue_account FOREIGN KEY (account_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- schedules
-- day_index: 0 = Mon ... 4 = Fri. group_code NULL = the class is for everyone.
CREATE TABLE IF NOT EXISTS class_sessions (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  day_index     TINYINT NOT NULL,
  name          VARCHAR(100) NOT NULL,
  start_time    TIME NOT NULL,
  end_time      TIME NOT NULL,
  room          VARCHAR(50) NOT NULL,
  session_type  ENUM('Lecture','Practical','Tutorial') NOT NULL,
  group_code    CHAR(3) NULL,
  KEY idx_session_day (day_index, start_time),
  CONSTRAINT fk_session_group FOREIGN KEY (group_code) REFERENCES lab_groups(group_code)
) ENGINE=InnoDB;

-- Same timetable as Timetable.java (only seeded into an empty table).
INSERT INTO class_sessions (day_index, name, start_time, end_time, room, session_type)
SELECT * FROM (
  SELECT 0 AS d, 'Database Systems' AS n, '09:00' AS s, '10:30' AS e, 'Room 102' AS r, 'Lecture' AS t UNION ALL
  SELECT 0, 'Mobile Dev Lab', '11:00', '12:30', 'Lab 3', 'Practical' UNION ALL
  SELECT 0, 'Computer Networks', '14:00', '15:30', 'Room 204', 'Lecture' UNION ALL
  SELECT 1, 'Software Engineering', '08:00', '09:30', 'Room 105', 'Lecture' UNION ALL
  SELECT 1, 'Data Structures & Algorithms', '10:00', '11:30', 'Room 204', 'Lecture' UNION ALL
  SELECT 1, 'Systems Analysis', '13:00', '14:00', 'Room 102', 'Tutorial' UNION ALL
  SELECT 2, 'Computer Networks Lab', '09:00', '11:00', 'Lab 2', 'Practical' UNION ALL
  SELECT 2, 'Database Systems', '11:30', '13:00', 'Room 102', 'Lecture' UNION ALL
  SELECT 3, 'Mobile Development', '09:00', '10:30', 'Room 301', 'Lecture' UNION ALL
  SELECT 3, 'Software Engineering Lab', '11:00', '12:30', 'Lab 3', 'Practical' UNION ALL
  SELECT 4, 'Computer Networks', '08:00', '09:30', 'Room 204', 'Lecture' UNION ALL
  SELECT 4, 'Research Methods', '10:00', '11:30', 'Room 110', 'Lecture'
) seed
WHERE NOT EXISTS (SELECT 1 FROM class_sessions);

-- ---------------------------------------------------------------- lecturers
CREATE TABLE IF NOT EXISTS lecturers (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  account_id  INT NOT NULL,
  department  VARCHAR(100) NULL,
  office      VARCHAR(50) NULL,
  UNIQUE KEY uq_lecturer_account (account_id),
  CONSTRAINT fk_lecturer_account FOREIGN KEY (account_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- password_resets
-- One-time codes for ResetPasswordActivity (otp.service.js). Only an HMAC of
-- the code is stored, never the code itself.
CREATE TABLE IF NOT EXISTS password_resets (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  email       VARCHAR(255) NOT NULL,
  otp_hash    CHAR(64) NOT NULL,
  attempts    INT NOT NULL DEFAULT 0,
  expires_at  DATETIME NOT NULL,
  used_at     DATETIME NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_reset_email (email, created_at)
) ENGINE=InnoDB;
