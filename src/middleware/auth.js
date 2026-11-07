'use strict';

// Identity and role checks for every protected route.
// Tokens are HS256 JWTs signed with JWT_SECRET and carry { id, email, role, studentId }.
// `id` is the accounts.id of the caller.
const jwt = require('jsonwebtoken');
const { ROLES } = require('../constants');
const { sendError } = require('../util/http');
const pool = require('../db/pool');

const IS_TEST = process.env.NODE_ENV === 'test';
const MIN_SECRET_LENGTH = 32;
const KNOWN_ROLES = Object.values(ROLES);

function loadSecret() {
  const secret = process.env.JWT_SECRET;
  if (secret && secret.length >= MIN_SECRET_LENGTH) return secret;
  // Jest runs with NODE_ENV=test, so the test suite can run without a real secret.
  if (IS_TEST) return 'test-only-secret-not-for-production-use';
  // Refuse to start with a missing or guessable secret rather than fall back to a default.
  throw new Error(
    `JWT_SECRET must be set to at least ${MIN_SECRET_LENGTH} random characters. ` +
    'Add it to .env (see .env.example).'
  );
}

const JWT_SECRET = loadSecret();

// Checks the signature, then checks the account in the database: the token must still
// carry the account's current token_version, and the account must still be active.
// So logout (and deactivation) revokes tokens immediately, not after 12 hours.
async function requireAuth(req, res, next) {
  const match = /^Bearer\s+(\S+)$/.exec(req.headers.authorization || '');
  if (!match) return sendError(res, 401, 'UNAUTHENTICATED', 'Missing bearer token');

  let payload;
  try {
    payload = jwt.verify(match[1], JWT_SECRET, { algorithms: ['HS256'] });
  } catch {
    return sendError(res, 401, 'INVALID_TOKEN', 'Token is invalid or expired. Log in again.');
  }

  if (!Number.isInteger(payload.id) || !KNOWN_ROLES.includes(payload.role)) {
    return sendError(res, 401, 'INVALID_TOKEN', 'Token is malformed. Log in again.');
  }

  const [[account]] = await pool.query(
    'SELECT token_version, is_active FROM accounts WHERE id = ?',
    [payload.id]
  );
  // A token from before token_version existed has no `v`, so it fails here and the user signs in again.
  if (!account || !account.is_active || payload.v !== account.token_version) {
    return sendError(res, 401, 'TOKEN_REVOKED', 'Your session has ended. Sign in again.');
  }

  req.user = {
    id: payload.id,
    email: payload.email,
    role: payload.role,
    studentId: payload.studentId || null,
  };
  next();
}

// Usage: requireRole(ROLES.LECTURER) or requireRole(ROLES.STUDENT, ROLES.LECTURER).
// Must come after requireAuth.
function requireRole(...allowed) {
  return (req, res, next) => {
    if (!req.user || !allowed.includes(req.user.role)) {
      return sendError(res, 403, 'FORBIDDEN', `This action needs the ${allowed.join(' or ')} role`);
    }
    next();
  };
}

module.exports = { requireAuth, requireRole, JWT_SECRET };
