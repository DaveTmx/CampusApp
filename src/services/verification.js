'use strict';

// Email-verification tokens. Only the SHA-256 hash is stored, so a leaked
// database cannot be used to verify accounts.
const crypto = require('crypto');

const RESEND_COOLDOWN_SECONDS = 60;

function hashToken(token) {
  return crypto.createHash('sha256').update(token, 'utf8').digest('hex');
}

// Must run inside a transaction. Marks older unused links as used, so only the
// newest link works, then stores a new hashed token valid for 24 hours.
// Returns the raw token, which is only ever sent by email.
async function issueVerificationToken(conn, accountId) {
  await conn.query(
    'UPDATE email_verification_tokens SET used_at = NOW() WHERE account_id = ? AND used_at IS NULL',
    [accountId]
  );
  const token = crypto.randomBytes(32).toString('hex');
  await conn.query(
    'INSERT INTO email_verification_tokens (token_hash, account_id, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 24 HOUR))',
    [hashToken(token), accountId]
  );
  return token;
}

// Must run inside a transaction. Burns older unused reset links, then stores a hashed token
// valid for 30 minutes. Returns the raw token, which is only ever sent by email.
const PASSWORD_RESET_MINUTES = 30;

async function issuePasswordResetToken(conn, accountId) {
  await conn.query(
    'UPDATE password_reset_tokens SET used_at = NOW() WHERE account_id = ? AND used_at IS NULL',
    [accountId]
  );
  const token = crypto.randomBytes(32).toString('hex');
  await conn.query(
    'INSERT INTO password_reset_tokens (token_hash, account_id, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE))',
    [hashToken(token), accountId, PASSWORD_RESET_MINUTES]
  );
  return token;
}

// ---- Staff sign-in codes (second factor for lecturers) ----
//
// A 6-digit code has only a million possibilities, so a plain SHA-256 could be
// reversed offline from a leaked table. The code is therefore keyed with HMAC
// using the server's JWT_SECRET, and bound to its challenge id.
const { JWT_SECRET } = require('../middleware/auth');

const CODE_TTL_MINUTES = 10;
const CODE_MAX_ATTEMPTS = 5;
const CODE_RESEND_SECONDS = 60;

function generateLoginCode() {
  // crypto.randomInt is uniform; padStart keeps leading zeros.
  return String(crypto.randomInt(0, 1000000)).padStart(6, '0');
}

function hashLoginCode(challengeId, code) {
  return crypto.createHmac('sha256', JWT_SECRET).update(`${challengeId}:${code}`, 'utf8').digest('hex');
}

// Compares two hex hashes in constant time.
function sameHash(a, b) {
  const x = Buffer.from(a, 'hex');
  const y = Buffer.from(b, 'hex');
  return x.length === y.length && crypto.timingSafeEqual(x, y);
}

// Must run inside a transaction. Older unused challenges stay valid until they
// expire or hit the attempt limit, so the newest one is the only one we mint.
async function issueLoginChallenge(conn, accountId) {
  const challengeId = crypto.randomUUID();
  const code = generateLoginCode();
  await conn.query(
    `INSERT INTO staff_login_challenges (challenge_id, account_id, code_hash, expires_at)
     VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE))`,
    [challengeId, accountId, hashLoginCode(challengeId, code), CODE_TTL_MINUTES]
  );
  return { challengeId, code };
}

module.exports = {
  hashToken,
  issueVerificationToken,
  issuePasswordResetToken,
  PASSWORD_RESET_MINUTES,
  RESEND_COOLDOWN_SECONDS,
  CODE_TTL_MINUTES,
  CODE_MAX_ATTEMPTS,
  CODE_RESEND_SECONDS,
  generateLoginCode,
  hashLoginCode,
  sameHash,
  issueLoginChallenge,
};
