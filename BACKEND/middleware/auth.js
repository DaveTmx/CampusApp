// Verifies identity (JWT) and role on protected routes. The server checks
// identity, role and record ownership on every protected request; the
// ownership checks live in the controllers.
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me'; // set a real one in .env

function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ success: false, message: 'Missing bearer token' });
  try {
    req.user = jwt.verify(token, JWT_SECRET); // { id, email, role }
    next();
  } catch (err) {
    return res.status(401).json({ success: false, message: 'Invalid or expired token' });
  }
}

function requireRole(role) {
  return (req, res, next) => {
    if (!req.user || req.user.role !== role) {
      return res.status(403).json({ success: false, message: `Requires ${role} role` });
    }
    next();
  };
}

module.exports = { requireAuth, requireRole, JWT_SECRET };
