// PATCH NOTE, not a new route file: authController.js's login() currently
// returns user info but no token, so there is nothing for requireAuth to
// check -- every protected route would be unreachable. Apply this change
// inside the existing login() function in src/controllers/authController.js:
//
//   const jwt = require('jsonwebtoken');
//   const { JWT_SECRET } = require('../middleware/auth');
//   ...
//   const token = jwt.sign(
//     { id: user.id, email: user.email, role: user.role || 'student' },
//     JWT_SECRET,
//     { expiresIn: '12h' }
//   );
//   res.status(200).json({
//     success: true,
//     message: 'Login successful',
//     token,
//     user: { id: user.id, full_name: user.full_name, email: user.email, role: user.role || 'student' },
//   });
//
// This also assumes `users` gets a `role` column (ENUM('student','lecturer')),
// which isn't in the current schema either -- add it alongside the
// students/operations tables in database/schema.sql:
//   ALTER TABLE users ADD COLUMN role ENUM('student','lecturer') NOT NULL DEFAULT 'student';
// and seed one lecturer row, per the lab's "seed one lecturer account" rule.
