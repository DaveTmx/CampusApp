const crypto = require('crypto');
const pool = require('../config/db');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require('../middleware/auth');
const { issueOtp, verifyOtp } = require('../services/otp.service');
const { withTransaction } = require('../utils/sql');

const PROGRAMMES = ['CS', 'IT', 'DS'];
const NUMBER_RE = /^\d{9}$/;
const MIN_PASSWORD_LENGTH = 6;

// Register a new user.
// Required: full_name, email, password.
// Optional (student sign-up): student_number (9 digits) + programme_code (CS/IT/DS).
//   If a roster row with that number already exists and has no login yet, the new
//   account is linked to it; otherwise a new roster row is created.
// The role is always 'student'. Lecturers are promoted in the database.
exports.register = async (req, res) => {
  const { full_name, email, password, student_number, programme_code } = req.body;

  if (!full_name || !email || !password) {
    return res.status(400).json({ success: false, message: 'Please provide full name, email, and password' });
  }
  if (String(password).length < MIN_PASSWORD_LENGTH) {
    return res.status(400).json({ success: false, message: `Password must be at least ${MIN_PASSWORD_LENGTH} characters` });
  }
  if (student_number !== undefined) {
    if (!NUMBER_RE.test(String(student_number))) {
      return res.status(400).json({ success: false, message: 'Student number must be exactly 9 digits' });
    }
    if (!PROGRAMMES.includes(programme_code)) {
      return res.status(400).json({ success: false, message: 'programme_code must be CS, IT or DS' });
    }
  }

  try {
    // Check if user already exists
    const [existing] = await pool.query('SELECT id FROM users WHERE email = ?', [email]);
    if (existing.length > 0) {
      return res.status(400).json({ success: false, message: 'Email is already registered' });
    }

    // Hash the password
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const userId = await withTransaction(pool, async (conn) => {
      const [result] = await conn.query(
        "INSERT INTO users (full_name, email, password, role) VALUES (?, ?, ?, 'student')",
        [full_name, email, hashedPassword]
      );

      if (student_number !== undefined) {
        const [[row]] = await conn.query(
          'SELECT student_id, account_id, deleted_at FROM students WHERE student_number = ? FOR UPDATE',
          [String(student_number)]
        );
        if (row) {
          if (row.deleted_at || row.account_id) {
            throw Object.assign(new Error('NUMBER_TAKEN'), { business: 'NUMBER_TAKEN' });
          }
          await conn.query('UPDATE students SET account_id = ? WHERE student_id = ?', [result.insertId, row.student_id]);
        } else {
          await conn.query(
            `INSERT INTO students (student_id, student_name, student_number, programme_code, account_id)
             VALUES (?, ?, ?, ?, ?)`,
            [crypto.randomUUID(), full_name.trim(), String(student_number), programme_code, result.insertId]
          );
        }
      }
      return result.insertId;
    });

    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      userId
    });
  } catch (error) {
    if (error.business === 'NUMBER_TAKEN' || error.code === 'ER_DUP_ENTRY') {
      return res.status(400).json({ success: false, message: 'That student number or email is already registered' });
    }
    console.error('Registration error:', error);
    res.status(500).json({ success: false, message: 'Server error during registration' });
  }
};

// Login user. Body: { email, password } or { student_number, password }
// (the Android student screen signs in with the student number).
exports.login = async (req, res) => {
  const { email, student_number, password } = req.body;

  if ((!email && !student_number) || !password) {
    return res.status(400).json({ success: false, message: 'Please provide email and password' });
  }

  try {
    // Find user by email, or by the student number linked to the account
    let rows;
    if (email) {
      [rows] = await pool.query('SELECT * FROM users WHERE email = ?', [email]);
    } else {
      [rows] = await pool.query(
        `SELECT u.* FROM users u
           JOIN students s ON s.account_id = u.id
          WHERE s.student_number = ? AND s.deleted_at IS NULL`,
        [String(student_number)]
      );
    }
    if (rows.length === 0) {
      return res.status(400).json({ success: false, message: 'Invalid credentials' });
    }

    const user = rows[0];

    // Check password
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(400).json({ success: false, message: 'Invalid credentials' });
    }

    // Protected routes (middleware/auth.js) need a signed token to check.
    const role = user.role || 'student';
    const token = jwt.sign({ id: user.id, email: user.email, role }, JWT_SECRET, { expiresIn: '12h' });

    res.status(200).json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        full_name: user.full_name,
        email: user.email,
        role
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ success: false, message: 'Server error during login' });
  }
};

// POST /api/auth/forgot-password  { email }
// Always answers the same way, so it cannot be used to discover which emails exist.
exports.forgotPassword = async (req, res) => {
  const email = String(req.body.email || '').trim();
  if (!email) {
    return res.status(400).json({ success: false, message: 'Please provide your email' });
  }
  try {
    await issueOtp(email);
    res.status(200).json({ success: true, message: 'If that email is registered, a 6-digit code has been sent' });
  } catch (error) {
    console.error('forgotPassword error:', error);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// POST /api/auth/reset-password  { email, otp, newPassword }
exports.resetPassword = async (req, res) => {
  const email = String(req.body.email || '').trim();
  const otp = String(req.body.otp || '').trim();
  const newPassword = String(req.body.newPassword || '');

  if (!email || !/^\d{6}$/.test(otp)) {
    return res.status(400).json({ success: false, message: 'Enter your email and the valid 6-digit OTP' });
  }
  if (newPassword.length < MIN_PASSWORD_LENGTH) {
    return res.status(400).json({ success: false, message: `Password must be at least ${MIN_PASSWORD_LENGTH} characters` });
  }

  try {
    if (!(await verifyOtp(email, otp))) {
      return res.status(400).json({ success: false, message: 'Invalid or expired code' });
    }
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(newPassword, salt);
    await pool.query('UPDATE users SET password = ? WHERE email = ?', [hashedPassword, email]);
    res.status(200).json({ success: true, message: 'Password reset successfully' });
  } catch (error) {
    console.error('resetPassword error:', error);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};
