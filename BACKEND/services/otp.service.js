// One-time passcodes for the "Forgot password?" flow (ResetPasswordActivity:
// enter email -> receive 6-digit OTP -> enter OTP + new password).
//  - 6 digits, valid 10 minutes, single use, 5 wrong tries then it is dead.
//  - Only an HMAC of the code is stored.
//  - There is no mail provider in this project, so outside production the
//    code is printed in the server console. Wire deliverOtp() to real email
//    or SMS before deploying.
const crypto = require('crypto');
const pool = require('../config/db');

const OTP_TTL_MINUTES = 10;
const MAX_ATTEMPTS = 5;
const RESEND_COOLDOWN_SECONDS = 60;
const OTP_SECRET = process.env.OTP_SECRET || process.env.JWT_SECRET || 'dev-secret-change-me';

function hashOtp(email, otp) {
  return crypto.createHmac('sha256', OTP_SECRET).update(`${email}:${otp}`).digest('hex');
}

function deliverOtp(email, otp) {
  if (process.env.NODE_ENV === 'production') {
    console.warn(`[OTP] No mail transport configured; code for ${email} was NOT delivered.`);
    return;
  }
  console.log(`[OTP] Password reset code for ${email}: ${otp} (valid ${OTP_TTL_MINUTES} min)`);
}

/**
 * Creates and delivers a code. Returns { issued: true } or
 * { issued: false, reason: 'NO_SUCH_USER' | 'THROTTLED' }. The controller
 * answers every case the same way so the endpoint cannot be used to find out
 * which emails are registered.
 */
async function issueOtp(email) {
  const [users] = await pool.query('SELECT id FROM users WHERE email = ?', [email]);
  if (users.length === 0) return { issued: false, reason: 'NO_SUCH_USER' };

  const [recent] = await pool.query(
    `SELECT id FROM password_resets
      WHERE email = ? AND created_at > DATE_SUB(NOW(), INTERVAL ? SECOND)`,
    [email, RESEND_COOLDOWN_SECONDS]
  );
  if (recent.length > 0) return { issued: false, reason: 'THROTTLED' };

  const otp = String(crypto.randomInt(0, 1000000)).padStart(6, '0');

  // Only the newest code is ever valid.
  await pool.query('UPDATE password_resets SET used_at = NOW() WHERE email = ? AND used_at IS NULL', [email]);
  await pool.query(
    `INSERT INTO password_resets (email, otp_hash, expires_at)
     VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE))`,
    [email, hashOtp(email, otp), OTP_TTL_MINUTES]
  );
  deliverOtp(email, otp);
  return { issued: true };
}

/** True only for the newest, unexpired, unused code, within the attempt limit. Consumes it on success. */
async function verifyOtp(email, otp) {
  if (!/^\d{6}$/.test(String(otp))) return false;

  const [rows] = await pool.query(
    `SELECT id, otp_hash, attempts FROM password_resets
      WHERE email = ? AND used_at IS NULL AND expires_at > NOW()
      ORDER BY id DESC LIMIT 1`,
    [email]
  );
  if (rows.length === 0) return false;
  const row = rows[0];
  if (row.attempts >= MAX_ATTEMPTS) return false;

  await pool.query('UPDATE password_resets SET attempts = attempts + 1 WHERE id = ?', [row.id]);

  const expected = Buffer.from(row.otp_hash, 'hex');
  const actual = Buffer.from(hashOtp(email, String(otp)), 'hex');
  if (expected.length !== actual.length || !crypto.timingSafeEqual(expected, actual)) return false;

  // Single use: only one concurrent request can flip used_at from NULL.
  const [upd] = await pool.query('UPDATE password_resets SET used_at = NOW() WHERE id = ? AND used_at IS NULL', [row.id]);
  return upd.affectedRows === 1;
}

module.exports = { issueOtp, verifyOtp, OTP_TTL_MINUTES };
