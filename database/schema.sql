-- Campus Connect backend schema (MySQL 8, InnoDB). Target database: ict361_lab
-- (matches the DB_NAME default in src/db/pool.js).
--
-- This replaces the earlier version, which referenced a `users` table that was
-- never created. If you already have an older ict361_lab database, back it up
-- and drop the old `students` and `operations` tables first: their columns differ.
-- Passwords and email-verification tokens are stored as hashes only.

CREATE TABLE IF NOT EXISTS programmes (
  programme_code  CHAR(4) PRIMARY KEY,
  programme_name  VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

INSERT IGNORE INTO programmes (programme_code, programme_name) VALUES
  ('CS',  'Computer Science'),
  ('IT',  'Information Technology'),
  ('DS',  'Data Science'),
  ('CYB', 'Cyber Security');

-- ICT361 lab groups G01-G14 with capacity 16. Assignment is capacity-checked
-- under SELECT ... FOR UPDATE (see assignToGroupTx in studentsController.js).
CREATE TABLE IF NOT EXISTS lab_groups (
  group_code  CHAR(3) PRIMARY KEY,
  capacity    INT NOT NULL DEFAULT 16
) ENGINE=InnoDB;

INSERT IGNORE INTO lab_groups (group_code, capacity) VALUES
  ('G01', 16), ('G02', 16), ('G03', 16), ('G04', 16), ('G05', 16), ('G06', 16), ('G07', 16),
  ('G08', 16), ('G09', 16), ('G10', 16), ('G11', 16), ('G12', 16), ('G13', 16), ('G14', 16);

-- Login accounts. role is set by the server only: self-registration always
-- creates 'student'; 'lecturer' accounts come from scripts/create-lecturer.js.
-- is_active stays 0 until the email-verification link has been opened.
CREATE TABLE IF NOT EXISTS accounts (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  email           VARCHAR(255) NOT NULL,            -- stored lower-case
  password_hash   VARCHAR(255) NOT NULL,            -- bcrypt
  full_name       VARCHAR(100) NOT NULL,
  role            ENUM('student','lecturer') NOT NULL,
  is_active       TINYINT(1) NOT NULL DEFAULT 0,
  failed_logins   INT NOT NULL DEFAULT 0,
  locked_until    DATETIME NULL,
  token_version   INT NOT NULL DEFAULT 0,          -- bumped on logout; older tokens stop working
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_accounts_email (email)
) ENGINE=InnoDB;

-- One row per registered or provisioned student record. account_id links a
-- student to their own login (students may edit only their own row).
CREATE TABLE IF NOT EXISTS students (
  student_id      CHAR(36) PRIMARY KEY,
  student_name    VARCHAR(100) NOT NULL,
  student_number  CHAR(9) NOT NULL,
  programme_code  CHAR(4) NOT NULL,
  group_code      CHAR(3) NULL,
  account_id      INT NULL,
  claim_code_hash CHAR(64) NULL,                    -- SHA-256 of the one-time claim code
  claimed_at      DATETIME NULL,
  version         INT NOT NULL DEFAULT 1,
  deleted_at      DATETIME NULL,                    -- soft delete; the number stays reserved
  UNIQUE KEY uq_student_number (student_number),
  UNIQUE KEY uq_students_account (account_id),
  CONSTRAINT fk_student_programme FOREIGN KEY (programme_code) REFERENCES programmes(programme_code),
  CONSTRAINT fk_student_group     FOREIGN KEY (group_code)     REFERENCES lab_groups(group_code),
  CONSTRAINT fk_student_account   FOREIGN KEY (account_id)     REFERENCES accounts(id)
) ENGINE=InnoDB;

-- Single-use email-verification links. Only the SHA-256 of the token is stored.
-- Valid for 24 hours; issuing a new link marks older unused links as used.
CREATE TABLE IF NOT EXISTS email_verification_tokens (
  token_hash  CHAR(64) PRIMARY KEY,
  account_id  INT NOT NULL,
  expires_at  DATETIME NOT NULL,
  used_at     DATETIME NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_evt_account (account_id, created_at),
  CONSTRAINT fk_evt_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Password-reset links. Only the SHA-256 hash of the token is stored; a link is single-use
-- and expires after 30 minutes. Requesting a new link burns the older unused ones.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  token_hash  CHAR(64) PRIMARY KEY,
  account_id  INT NOT NULL,
  expires_at  DATETIME NOT NULL,
  used_at     DATETIME NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_prt_account (account_id, created_at),
  CONSTRAINT fk_prt_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Lecturer sign-in codes (second factor). One row per password check that passed.
-- Only an HMAC of the code is stored (keyed with JWT_SECRET), never the code itself.
-- Codes expire after 10 minutes and are burned after 5 wrong guesses.
CREATE TABLE IF NOT EXISTS staff_login_challenges (
  challenge_id  CHAR(36) PRIMARY KEY,
  account_id    INT NOT NULL,
  code_hash     CHAR(64) NOT NULL,
  attempts      INT NOT NULL DEFAULT 0,
  expires_at    DATETIME NOT NULL,
  used_at       DATETIME NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_slc_account (account_id, created_at),
  CONSTRAINT fk_slc_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tombstones for deleted students, so an offline device can learn about a
-- deletion when it syncs. Written in the same transaction as the soft delete.
CREATE TABLE IF NOT EXISTS deletion_markers (
  student_id  CHAR(36) PRIMARY KEY,
  deleted_at  DATETIME NOT NULL,
  deleted_by  INT NULL
) ENGINE=InnoDB;

-- Idempotency receipts: one row per client-generated operationId. The receipt
-- is written in the same transaction as the change, so they commit together.
-- payload_json stores the request fingerprint {opType, payload}, canonicalised.
CREATE TABLE IF NOT EXISTS operations (
  operation_id  CHAR(36) PRIMARY KEY,
  account_id    INT NULL,
  student_id    CHAR(36) NULL,
  op_type       VARCHAR(20) NOT NULL,
  payload_json  JSON NOT NULL,
  http_status   SMALLINT UNSIGNED NOT NULL,
  status        ENUM('APPLIED','FAILED') NOT NULL,
  result_json   JSON NOT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Campus-life collaboration groups (GroupsActivity). Separate from lab_groups.
CREATE TABLE IF NOT EXISTS study_groups (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  description  VARCHAR(500) NULL,
  course_code  VARCHAR(20) NULL,
  created_by   INT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
