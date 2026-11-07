'use strict';

// Lecturer sign-in: password first, then a 6-digit code emailed by the backend.
//
// Step 1  POST /api/auth/staff/login        { email, password }  -> 202 { challengeId }
// Step 2  POST /api/auth/staff/verify-code  { challengeId, code } -> 200 { token, user }
//
// Only lecturer accounts are accepted here. Student accounts use /api/auth/login.
// Wrong passwords and wrong codes count toward the same lockout (5 failures, 15 minutes),
// so a locked account cannot ask for new codes either.
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db/pool');
const { inTransaction } = require('../db/tx');
const { JWT_SECRET } = require('../middleware/auth');
const { LOCKOUT, ROLES } = require('../constants');
const {
  CODE_TTL_MINUTES,
  CODE_MAX_ATTEMPTS,
  CODE_RESEND_SECONDS,
  hashLoginCode,
  sameHash,
  issueLoginChallenge,
} = require('../services/verification');
const { sendStaffLoginCode } = require('../mailer');
const { sendError } = require('../util/http');
const { normalizeEmail, isValidEmail } = require('../util/validation');

const BCRYPT_ROUNDS = 12;
const SESSION_SECONDS = 12 * 60 * 60;
const LOCKED_MESSAGE = `Too many failed attempts. Try again in ${LOCKOUT.MINUTES} minutes.`;
const CHALLENGE_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const CODE_FORMAT = /^\d{6}$/;

let dummyHashPromise;
function getDummyHash() {
  dummyHashPromise ??= bcrypt.hash('not-a-real-password', BCRYPT_ROUNDS);
  return dummyHashPromise;
}

// POST /api/auth/staff/login
exports.staffLogin = async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const password = typeof req.body?.password === 'string' ? req.body.password : '';
  if (!email || !password) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Email and password are required');
  }

  const [[account]] = await pool.query(
    `SELECT id, email, full_name, role, password_hash, is_active, failed_logins,
            (locked_until IS NOT NULL AND locked_until > NOW()) AS is_locked
       FROM accounts WHERE email = ?`,
    [email]
  );
  const isStaff = Boolean(account) && account.role === ROLES.LECTURER;

  // Same bcrypt cost whether or not the email exists, so timing does not reveal accounts.
  const passwordOk = await bcrypt.compare(password, isStaff ? account.password_hash : await getDummyHash());
  if (!isStaff) {
    return sendError(res, 401, 'INVALID_CREDENTIALS', 'Email or password is incorrect');
  }
  if (account.is_locked) {
    return sendError(res, 423, 'ACCOUNT_LOCKED', LOCKED_MESSAGE);
  }
  if (!passwordOk) {
    const failures = account.failed_logins + 1;
    if (failures >= LOCKOUT.MAX_FAILED) {
      await pool.query(
        'UPDATE accounts SET failed_logins = 0, locked_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id = ?',
        [LOCKOUT.MINUTES, account.id]
      );
      return sendError(res, 423, 'ACCOUNT_LOCKED', LOCKED_MESSAGE);
    }
    await pool.query('UPDATE accounts SET failed_logins = ? WHERE id = ?', [failures, account.id]);
    return sendError(res, 401, 'INVALID_CREDENTIALS', 'Email or password is incorrect');
  }
  if (!account.is_active) {
    return sendError(res, 403, 'EMAIL_NOT_VERIFIED', 'Verify your email before logging in. Check your inbox or request a new link.');
  }

  // Password is correct. Issue a code, unless one was sent very recently.
  const issued = await inTransaction(async (conn) => {
    // Lock the account so two requests cannot both pass the cooldown check.
    await conn.query('SELECT id FROM accounts WHERE id = ? FOR UPDATE', [account.id]);
    const [[last]] = await conn.query(
      'SELECT TIMESTAMPDIFF(SECOND, MAX(created_at), NOW()) AS age FROM staff_login_challenges WHERE account_id = ?',
      [account.id]
    );
    if (last.age !== null && last.age < CODE_RESEND_SECONDS) {
      return { tooSoon: true, retryAfter: CODE_RESEND_SECONDS - last.age };
    }
    return issueLoginChallenge(conn, account.id);
  });

  if (issued.tooSoon) {
    return sendError(res, 429, 'CODE_ALREADY_SENT', 'A code was sent a moment ago. Wait before asking for another.', {
      retryAfterSeconds: issued.retryAfter,
    });
  }

  try {
    await sendStaffLoginCode({ to: account.email, fullName: account.full_name, code: issued.code });
  } catch (err) {
    // Without the email the code is useless, so burn the challenge and report the failure.
    console.error('staff sign-in code not sent:', err.message);
    await pool.query('UPDATE staff_login_challenges SET used_at = NOW() WHERE challenge_id = ?', [issued.challengeId]);
    return sendError(res, 503, 'CODE_NOT_SENT', 'The sign-in code could not be emailed. Try again shortly.');
  }

  res.status(202).json({
    success: true,
    status: 'CODE_SENT',
    challengeId: issued.challengeId,
    expiresInSeconds: CODE_TTL_MINUTES * 60,
    message: 'A 6-digit code has been emailed to you.',
  });
};

// POST /api/auth/staff/verify-code
exports.staffVerifyCode = async (req, res) => {
  const challengeId = typeof req.body?.challengeId === 'string' ? req.body.challengeId.toLowerCase() : '';
  const code = typeof req.body?.code === 'string' ? req.body.code.trim() : '';
  if (!CHALLENGE_ID.test(challengeId) || !CODE_FORMAT.test(code)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Enter the 6-digit code from the email');
  }

  const outcome = await inTransaction(async (conn) => {
    // Lock both the challenge and its account, so guesses cannot race past the limits.
    const [[row]] = await conn.query(
      `SELECT c.code_hash, c.attempts, c.used_at, (c.expires_at > NOW()) AS still_valid,
              a.id AS account_id, a.email, a.full_name, a.role, a.is_active, a.failed_logins, a.token_version,
              (a.locked_until IS NOT NULL AND a.locked_until > NOW()) AS is_locked
         FROM staff_login_challenges c
         JOIN accounts a ON a.id = c.account_id
        WHERE c.challenge_id = ?
          FOR UPDATE`,
      [challengeId]
    );

    if (!row || row.used_at || !row.still_valid || row.attempts >= CODE_MAX_ATTEMPTS
        || row.role !== ROLES.LECTURER || !row.is_active) {
      return { kind: 'INVALID' };
    }
    if (row.is_locked) {
      return { kind: 'LOCKED' };
    }

    if (!sameHash(hashLoginCode(challengeId, code), row.code_hash)) {
      const attempts = row.attempts + 1;
      await conn.query('UPDATE staff_login_challenges SET attempts = ? WHERE challenge_id = ?', [attempts, challengeId]);
      if (attempts >= CODE_MAX_ATTEMPTS) {
        await conn.query('UPDATE staff_login_challenges SET used_at = NOW() WHERE challenge_id = ?', [challengeId]);
      }
      const failures = row.failed_logins + 1;
      if (failures >= LOCKOUT.MAX_FAILED) {
        await conn.query(
          'UPDATE accounts SET failed_logins = 0, locked_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id = ?',
          [LOCKOUT.MINUTES, row.account_id]
        );
        return { kind: 'LOCKED' };
      }
      await conn.query('UPDATE accounts SET failed_logins = ? WHERE id = ?', [failures, row.account_id]);
      return { kind: 'WRONG', attemptsLeft: Math.max(0, CODE_MAX_ATTEMPTS - attempts) };
    }

    // Correct code: single use. Clear the failure counter.
    await conn.query('UPDATE staff_login_challenges SET used_at = NOW() WHERE challenge_id = ?', [challengeId]);
    await conn.query('UPDATE accounts SET failed_logins = 0, locked_until = NULL WHERE id = ?', [row.account_id]);
    return { kind: 'OK', account: row };
  });

  // The transaction commits on return, so failed guesses are kept even though we answer with an error.
  if (outcome.kind === 'INVALID') {
    return sendError(res, 401, 'CODE_INVALID', 'This code is no longer valid. Sign in again to get a new one.');
  }
  if (outcome.kind === 'LOCKED') {
    return sendError(res, 423, 'ACCOUNT_LOCKED', LOCKED_MESSAGE);
  }
  if (outcome.kind === 'WRONG') {
    return sendError(res, 401, 'CODE_INCORRECT', 'That code is not correct.', { attemptsLeft: outcome.attemptsLeft });
  }

  const account = outcome.account;
  const token = jwt.sign(
    { id: account.account_id, email: account.email, role: ROLES.LECTURER, studentId: null, v: account.token_version },
    JWT_SECRET,
    { algorithm: 'HS256', expiresIn: SESSION_SECONDS }
  );
  res.json({
    success: true,
    message: 'Login successful',
    token,
    expiresIn: SESSION_SECONDS,
    user: {
      id: account.account_id,
      fullName: account.full_name,
      email: account.email,
      role: ROLES.LECTURER,
      studentId: null,
    },
  });
};
