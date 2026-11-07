#!/usr/bin/env node
'use strict';

// Creates a lecturer account. Lecturers cannot be created through the API.
//
// Usage:
//   APPROVED_LECTURER_EMAILS=lecturer@uni.edu LECTURER_INITIAL_PASSWORD='at-least-12-chars' \
//     node scripts/create-lecturer.js lecturer@uni.edu "Full Name"
//
// The account stays inactive until the lecturer opens the verification link sent
// to that email. That proves they control the mailbox. Unset LECTURER_INITIAL_PASSWORD
// once the command has run.
require('dotenv').config();
const bcrypt = require('bcryptjs');
const pool = require('../src/db/pool');
const { inTransaction } = require('../src/db/tx');
const { sendVerificationEmail } = require('../src/mailer');
const { issueVerificationToken } = require('../src/services/verification');
const { normalizeEmail, isValidEmail, cleanText } = require('../src/util/validation');
const { ROLES } = require('../src/constants');

function fail(message) {
  console.error(message);
  process.exitCode = 1;
  return null;
}

async function main() {
  const [, , rawEmail, ...nameParts] = process.argv;
  const email = normalizeEmail(rawEmail);
  const fullName = cleanText(nameParts.join(' '), 100);
  if (!isValidEmail(email) || !fullName) {
    return fail('Usage: node scripts/create-lecturer.js <email> "<Full Name>"');
  }

  const approved = (process.env.APPROVED_LECTURER_EMAILS || '')
    .split(',')
    .map(normalizeEmail)
    .filter(Boolean);
  if (!approved.includes(email)) {
    return fail('This email is not on APPROVED_LECTURER_EMAILS. Add it only after the department confirms it.');
  }

  const initialPassword = process.env.LECTURER_INITIAL_PASSWORD || '';
  if (initialPassword.length < 12) {
    return fail('Set LECTURER_INITIAL_PASSWORD to at least 12 characters.');
  }
  const passwordHash = await bcrypt.hash(initialPassword, 12);

  const token = await inTransaction(async (conn) => {
    const [[existing]] = await conn.query('SELECT id FROM accounts WHERE email = ? FOR UPDATE', [email]);
    if (existing) throw new Error('An account with this email already exists.');
    const [account] = await conn.query(
      'INSERT INTO accounts (email, password_hash, full_name, role, is_active) VALUES (?, ?, ?, ?, 0)',
      [email, passwordHash, fullName, ROLES.LECTURER]
    );
    return issueVerificationToken(conn, account.insertId);
  });

  await sendVerificationEmail({ to: email, fullName, token });
  console.log(`Lecturer account created for ${email}. A verification email was sent.`);
}

main()
  .catch((err) => {
    console.error(err.message);
    process.exitCode = 1;
  })
  .finally(() => pool.end());
