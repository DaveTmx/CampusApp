'use strict';

// Registration, email verification, login and logout.
//
// Lecturer accounts are never created here. Registration always makes a
// 'student'. Lecturers are provisioned by scripts/create-lecturer.js, which only
// accepts emails on the server-side APPROVED_LECTURER_EMAILS list.
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db/pool');
const { inTransaction } = require('../db/tx');
const { JWT_SECRET } = require('../middleware/auth');
const { PROGRAMMES, LOCKOUT, ROLES } = require('../constants');
const { hashToken, issueVerificationToken, issuePasswordResetToken, RESEND_COOLDOWN_SECONDS } = require('../services/verification');
const { sendVerificationEmail, sendPasswordResetEmail } = require('../mailer');
const { sendError } = require('../util/http');

const BCRYPT_ROUNDS = 12;
const SESSION_SECONDS = 12 * 60 * 60;
const TOKEN_FORMAT = /^[a-f0-9]{64}$/;

// Used to spend the same bcrypt time when the email is unknown, so response
// timing does not reveal which emails have accounts.
let dummyHashPromise;
function getDummyHash() {
  dummyHashPromise ??= bcrypt.hash('not-a-real-password', BCRYPT_ROUNDS);
  return dummyHashPromise;
}

// POST /api/auth/register  { fullName, email, password, studentNumber, programmeCode }
// Creates an inactive student account and the matching student record, then emails a link.
exports.register = async (req, res) => {
  const body = req.body || {};
  const email = normalizeEmail(body.email);
  const password = typeof body.password === 'string' ? body.password : '';
  const studentNumber = typeof body.studentNumber === 'string' ? body.studentNumber.trim() : '';
  const programmeCode = typeof body.programmeCode === 'string' ? body.programmeCode.trim().toUpperCase() : '';

  const invalid = [];
  if (!fullName) invalid.push('fullName');
  if (!isValidEmail(email)) invalid.push('email');
  // bcrypt only uses the first 72 bytes, so longer passwords are refused rather than silently truncated.
  if (password.length < 8 || Buffer.byteLength(password, 'utf8') > 72) invalid.push('password');
  if (!/^\d{9}$/.test(studentNumber)) invalid.push('studentNumber');
  if (!PROGRAMMES.includes(programmeCode)) invalid.push('programmeCode');
  if (invalid.length > 0) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Some fields are missing or invalid', { fields: invalid });
  }

  const passwordHash = await bcrypt.hash(password, BCRYPT_ROUNDS);

  try {
      const [account] = await conn.query(
        'INSERT INTO accounts (email, password_hash, full_name, role, is_active) VALUES (?, ?, ?, ?, 0)',
        [email, passwordHash, fullName, ROLES.STUDENT]
      );
      await conn.query(
        'INSERT INTO students (student_id, student_name, student_number, programme_code, account_id) VALUES (?, ?, ?, ?, ?)',
        [randomUUID(), fullName, studentNumber, programmeCode, account.insertId]
      );
    });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY' && err.message.includes('uq_accounts_email')) {
      return sendError(res, 409, 'DUPLICATE_EMAIL', 'An account with this email already exists');
    }
    if (err.code === 'ER_DUP_ENTRY' && err.message.includes('uq_student_number')) {
      return sendError(res, 409, 'DUPLICATE_NUMBER', 'A student with this student number already exists');
    }
    throw err;
  }

  try {
    await sendVerificationEmail({ to: email, fullName, token });
  } catch (err) {
    // The account exists; the user can ask for another link.
    console.error('verification email not sent:', err.message);
  }

  res.status(201).json({
    success: true,
    status: 'PENDING_VERIFICATION',
    message: 'Account created. Open the link in the verification email before logging in.',
  });
};

// GET /api/auth/verify?token=...
// Activates the account. Links are single-use and expire after 24 hours.
exports.verifyEmail = async (req, res) => {
  const token = typeof req.query.token === 'string' ? req.query.token : '';
  if (!TOKEN_FORMAT.test(token)) {
    return sendError(res, 400, 'INVALID_TOKEN', 'This verification link is not valid');
  }

  const outcome = await inTransaction(async (conn) => {
    const [[row]] = await conn.query(
      'SELECT account_id, used_at, (expires_at > NOW()) AS still_valid FROM email_verification_tokens WHERE token_hash = ? FOR UPDATE',
      [hashToken(token)]
    );
    if (!row || row.used_at || !row.still_valid) {
      return { rollback: true, valid: false };
    }
    await conn.query('UPDATE email_verification_tokens SET used_at = NOW() WHERE token_hash = ?', [hashToken(token)]);
    await conn.query('UPDATE accounts SET is_active = 1 WHERE id = ?', [row.account_id]);
    return { valid: true };
  });

  if (!outcome.valid) {
    return sendError(res, 400, 'INVALID_TOKEN', 'This verification link is invalid or has expired. Request a new one.');
  }
  res.json({ success: true, message: 'Email verified. You can now log in.' });
};

// POST /api/auth/resend-verification  { email }
// Always answers the same way, so it cannot be used to discover accounts.
// A new link is sent at most once per RESEND_COOLDOWN_SECONDS per account.
exports.resendVerification = async (req, res) => {
  const generic = {
    success: true,
    message: 'If that account is waiting for verification, a new link has been sent.',
  };
  const email = normalizeEmail(req.body?.email);
  if (!isValidEmail(email)) return res.json(generic);

  const pending = await inTransaction(async (conn) => {
    // Lock the account row so two quick requests cannot both pass the cooldown check.
    const [[account]] = await conn.query(
      'SELECT id, full_name FROM accounts WHERE email = ? AND is_active = 0 FOR UPDATE',
      [email]
    );
    if (!account) return null;

    const [[last]] = await conn.query(
      'SELECT TIMESTAMPDIFF(SECOND, MAX(created_at), NOW()) AS age FROM email_verification_tokens WHERE account_id = ?',
      [account.id]
    );
    if (last.age !== null && last.age < RESEND_COOLDOWN_SECONDS) return null;

    const token = await issueVerificationToken(conn, account.id);
    return { to: email, fullName: account.full_name, token };
  });

  if (pending) {
    try {
      await sendVerificationEmail(pending);
    } catch (err) {
      console.error('resend verification email failed:', err.message);
    }
  }
  res.json(generic);
};

// POST /api/auth/login  { identifier, password }
// identifier is the account email, or a 9-digit student number. The older field name "email" is still accepted.
// Returns a 12-hour JWT. Five wrong passwords lock the account for 15 minutes, whichever identifier was used.
exports.login = async (req, res) => {
  const raw = typeof req.body?.identifier === 'string' ? req.body.identifier : req.body?.email;
  const identifier = typeof raw === 'string' ? raw.trim() : '';
  const password = typeof req.body?.password === 'string' ? req.body.password : '';
  if (!identifier || !password) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Username and password are required');
  }

  const isStudentNumber = /^\d{9}$/.test(identifier);
  const [[account]] = isStudentNumber
    ? await pool.query(
        `SELECT a.id, a.email, a.full_name, a.role, a.password_hash, a.is_active, a.failed_logins, a.token_version,
                (a.locked_until IS NOT NULL AND a.locked_until > NOW()) AS is_locked
           FROM students s JOIN accounts a ON a.id = s.account_id
          WHERE s.student_number = ? AND s.deleted_at IS NULL`,
        [identifier]
      )
    : await pool.query(
        `SELECT id, email, full_name, role, password_hash, is_active, failed_logins, token_version,
                (locked_until IS NOT NULL AND locked_until > NOW()) AS is_locked
           FROM accounts WHERE email = ?`,
        [normalizeEmail(identifier)]
      );

  const passwordOk = await bcrypt.compare(password, account ? account.password_hash : await getDummyHash());
  if (!account) {
    return sendError(res, 401, 'INVALID_CREDENTIALS', 'Email or password is incorrect');
  }

  if (account.is_locked) {
    return sendError(res, 423, 'ACCOUNT_LOCKED', `Too many failed attempts. Try again in ${LOCKOUT.MINUTES} minutes.`);
  }

  if (!passwordOk) {
    const failures = account.failed_logins + 1;
    if (failures >= LOCKOUT.MAX_FAILED) {
      await pool.query(
        'UPDATE accounts SET failed_logins = 0, locked_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id = ?',
        [LOCKOUT.MINUTES, account.id]
      );
      return sendError(res, 423, 'ACCOUNT_LOCKED', `Too many failed attempts. Try again in ${LOCKOUT.MINUTES} minutes.`);
    }
    await pool.query('UPDATE accounts SET failed_logins = ? WHERE id = ?', [failures, account.id]);
    return sendError(res, 401, 'INVALID_CREDENTIALS', 'Email or password is incorrect');
  }

  // Lecturers never get a token from this endpoint, even with the right password.
  // They must also pass the emailed code at /api/auth/staff/login.
  if (account.role === ROLES.LECTURER) {
    return sendError(res, 403, 'STAFF_SIGN_IN_REQUIRED', 'Lecturers sign in with a code sent by email. Use the staff sign-in.');
  }

  if (!account.is_active) {
    return sendError(res, 403, 'EMAIL_NOT_VERIFIED', 'Verify your email before logging in. Check your inbox or request a new link.');
  }

  await pool.query('UPDATE accounts SET failed_logins = 0, locked_until = NULL WHERE id = ?', [account.id]);
  const [[student]] = await pool.query(
    'SELECT student_id FROM students WHERE account_id = ? AND deleted_at IS NULL',
    [account.id]
  );
  const studentId = student ? student.student_id : null;

  const token = jwt.sign(
    { id: account.id, email: account.email, role: account.role, studentId, v: account.token_version },
    JWT_SECRET,
    { algorithm: 'HS256', expiresIn: SESSION_SECONDS }
  );

  res.json({
    success: true,
    message: 'Login successful',
    token,
    expiresIn: SESSION_SECONDS,
    user: {
      id: account.id,
      fullName: account.full_name,
      email: account.email,
      role: account.role,
      studentId,
    },
  });
};

// POST /api/auth/forgot-password  { email }
// Always answers the same way, so it cannot be used to discover accounts.
// Only active accounts get a link; each request burns the older links.
exports.forgotPassword = async (req, res) => {
  const generic = {
    success: true,
    message: 'If an account exists for that email, a reset link has been sent.',
  };
  const email = normalizeEmail(req.body?.email);
  if (!isValidEmail(email)) return res.json(generic);

  const pending = await inTransaction(async (conn) => {
    const [[account]] = await conn.query(
      'SELECT id, full_name FROM accounts WHERE email = ? AND is_active = 1 FOR UPDATE',
      [email]
    );
    if (!account) return null;
    const token = await issuePasswordResetToken(conn, account.id);
    return { to: email, fullName: account.full_name, token };
  });

  // Reply first. The mail server can be slow or drop the connection, and the phone should not wait for it.
  res.json(generic);

  if (pending) {
    Promise.resolve()
      .then(() => sendPasswordResetEmail(pending))
      .catch((err) => console.error('password reset email failed:', err.message));
  }
};

// Minimal page opened from the reset email. It posts to /api/auth/reset-password.
// The token is hex-checked before it is placed in the page, so nothing else can be injected.
exports.resetPasswordPage = (req, res) => {
  const token = typeof req.query.token === 'string' && TOKEN_FORMAT.test(req.query.token) ? req.query.token : '';
  res.type('html').send(resetPageHtml(token));
};

// POST /api/auth/reset-password  { token, password }
// Single-use, 30-minute links. Success signs out every existing session (token_version is bumped).
exports.resetPassword = async (req, res) => {
  const token = typeof req.body?.token === 'string' ? req.body.token : '';
  const password = typeof req.body?.password === 'string' ? req.body.password : '';
  if (!TOKEN_FORMAT.test(token)) {
    return sendError(res, 400, 'INVALID_TOKEN', 'This reset link is not valid. Request a new one.');
  }
  if (password.length < 8 || Buffer.byteLength(password, 'utf8') > 72) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Password must be 8 to 72 bytes long', { fields: ['password'] });
  }

  const passwordHash = await bcrypt.hash(password, BCRYPT_ROUNDS);
  const outcome = await inTransaction(async (conn) => {
    const [[row]] = await conn.query(
      'SELECT account_id, used_at, (expires_at > NOW()) AS still_valid FROM password_reset_tokens WHERE token_hash = ? FOR UPDATE',
      [hashToken(token)]
    );
    if (!row || row.used_at || !row.still_valid) {
      return { valid: false };
    }
    await conn.query('UPDATE password_reset_tokens SET used_at = NOW() WHERE token_hash = ?', [hashToken(token)]);
    await conn.query(
      'UPDATE accounts SET password_hash = ?, failed_logins = 0, locked_until = NULL, token_version = token_version + 1 WHERE id = ?',
      [passwordHash, row.account_id]
    );
    return { valid: true };
  });

  if (!outcome.valid) {
    return sendError(res, 400, 'INVALID_TOKEN', 'This reset link is invalid or has expired. Request a new one.');
  }
  res.json({ success: true, message: 'Your password has been changed. Sign in with the new password.' });
};

function resetPageHtml(token) {
  const body = token
    ? `<form id="f">
         <label>New password<input id="pw" type="password" minlength="8" required autocomplete="new-password"></label>
         <label>Confirm password<input id="pw2" type="password" minlength="8" required autocomplete="new-password"></label>
         <input id="token" type="hidden" value="${token}">
         <button type="submit">Save new password</button>
       </form>
       <p id="msg" role="status"></p>
       <script>
         document.getElementById('f').addEventListener('submit', async (e) => {
           e.preventDefault();
           const msg = document.getElementById('msg');
           const pw = document.getElementById('pw').value;
           if (pw !== document.getElementById('pw2').value) { msg.textContent = "The passwords do not match."; return; }
           msg.textContent = 'Saving...';
           const res = await fetch('/api/auth/reset-password', {
             method: 'POST',
             headers: { 'Content-Type': 'application/json' },
             body: JSON.stringify({ token: document.getElementById('token').value, password: pw })
           });
           const data = await res.json().catch(() => ({}));
           msg.textContent = res.ok ? data.message : (data.message || 'Something went wrong. Try again.');
         });
       </script>`
    : '<p>This reset link is not valid. Request a new one from the app.</p>';
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Reset password - Campus Connect</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 0; padding: 16px; background: #fff; color: #111; }
    h1 { font-size: 22px; }
    label { display: block; margin: 12px 0; font-size: 14px; }
    input { display: block; width: 100%; box-sizing: border-box; padding: 12px; margin-top: 4px; font-size: 16px; }
    button { width: 100%; padding: 14px; font-size: 16px; font-weight: bold; background: #0d47a1; color: #fff; border: 0; border-radius: 8px; margin-top: 12px; }
    #msg { font-size: 15px; }
  </style>
</head>
<body>
  <h1>Choose a new password</h1>
  ${body}
</body>
</html>`;
}

// POST /api/auth/logout (requires a token)
// Revokes every token this account holds on every device by bumping token_version.
exports.logout = async (req, res) => {
  await pool.query('UPDATE accounts SET token_version = token_version + 1 WHERE id = ?', [req.user.id]);
  res.json({ success: true, message: 'Logged out. All sessions for this account have ended.' });
};
