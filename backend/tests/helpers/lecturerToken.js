'use strict';

// Creates (or resets) a real lecturer account and returns a token for it.
// The token carries the account's current token_version, so requireAuth accepts it.
const jwt = require('jsonwebtoken');
const pool = require('../../../src/db/pool');
const { JWT_SECRET } = require('../../../src/middleware/auth');

async function makeLecturerToken() {
  await pool.query(
    `INSERT INTO accounts (email, password_hash, full_name, role, is_active, token_version)
     VALUES ('lecturer@test', 'not-used-for-login', 'Test Lecturer', 'lecturer', 1, 0)
     ON DUPLICATE KEY UPDATE role = 'lecturer', is_active = 1, locked_until = NULL, failed_logins = 0`
  );
  const [[account]] = await pool.query(
    "SELECT id, token_version FROM accounts WHERE email = 'lecturer@test'"
  );
  return jwt.sign(
    { id: account.id, email: 'lecturer@test', role: 'lecturer', studentId: null, v: account.token_version },
    JWT_SECRET,
    { algorithm: 'HS256', expiresIn: 3600 }
  );
}

module.exports = { makeLecturerToken };
