-- Run once, in MySQL Workbench, on your EXISTING database (ict361_lab).
-- (The claim_code_hash and claimed_at columns on students are already added.)
USE ict361_lab;

-- Password-reset links (new in the merged backend). Only the hash of the link token is stored.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  token_hash  CHAR(64) PRIMARY KEY,
  account_id  INT NOT NULL,
  expires_at  DATETIME NOT NULL,
  used_at     DATETIME NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_prt_account (account_id, created_at),
  CONSTRAINT fk_prt_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

SHOW TABLES;   -- expect 10 tables, including password_reset_tokens
