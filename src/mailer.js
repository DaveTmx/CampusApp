'use strict';

// Sends the account-verification email with Nodemailer.
//
// Configuration (see .env.example):
//   SMTP_HOST, SMTP_PORT (587 STARTTLS by default, 465 implicit TLS),
//   SMTP_USER, SMTP_PASS, SMTP_FROM, APP_URL (base URL used in the link).
//
// Without SMTP_HOST: outside production the link is printed to the console
// (development only); in production sending fails loudly.
// Under NODE_ENV=test nothing is sent; messages go to `outbox` for the tests to read.
const nodemailer = require('nodemailer');

const outbox = [];
let transporter; // undefined = not created yet, null = SMTP not configured

function getTransporter() {
  if (transporter !== undefined) return transporter;
  if (!process.env.SMTP_HOST) {
    transporter = null;
    return transporter;
  }
  const port = Number(process.env.SMTP_PORT || 587);
  transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port,
    secure: port === 465,      // implicit TLS on 465
    requireTLS: port !== 465,  // otherwise require STARTTLS: never send credentials in clear text
    auth: process.env.SMTP_USER
      ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS }
      : undefined,
  });
  return transporter;
}

function verificationLink(token) {
  const base = (process.env.APP_URL || 'http://localhost:5000').replace(/\/+$/, '');
  return `${base}/api/auth/verify?token=${encodeURIComponent(token)}`;
}

// Shared delivery path. Without SMTP, nothing is sent. In development only, and only
// when DEV_SHOW_CODES=1 is set on purpose, the secret (link or code) is printed so the
// flow can be tried locally. It is never printed in production or in tests.
async function deliver(message, { link, code }) {
  if (process.env.NODE_ENV === 'test') {
    outbox.push({ ...message, link, code });
    return;
  }

  const smtp = getTransporter();
  if (!smtp) {
    if (process.env.NODE_ENV === 'production') {
      throw new Error('SMTP is not configured; cannot send email');
    }
    const secret = link || code;
    const showSecret = process.env.DEV_SHOW_CODES === '1' && process.env.NODE_ENV !== 'production';
    console.log(`[dev mailer] SMTP not configured; email to ${message.to} was not sent` +
      (showSecret ? ` (dev only, DEV_SHOW_CODES=1): ${secret}` : ''));
    return;
  }
  await smtp.sendMail(message);
}

function fromAddress() {
  return process.env.SMTP_FROM || 'Campus Connect <no-reply@localhost>';
}

async function sendVerificationEmail({ to, fullName, token }) {
  const link = verificationLink(token);
  const message = {
    to,
    from: fromAddress(),
    subject: 'Verify your Campus Connect account',
    text:
      `Hi ${fullName},\n\n` +
      `Confirm your email address to activate your account:\n${link}\n\n` +
      'This link expires in 24 hours. If you did not create an account, ignore this email.',
  };
  await deliver(message, { link });
}

// Staff sign-in code. Sent only after the password has been checked.
async function sendStaffLoginCode({ to, fullName, code }) {
  const message = {
    to,
    from: fromAddress(),
    subject: 'Your Campus Connect sign-in code',
    text:
      `Hi ${fullName},\n\n` +
      `Your sign-in code is: ${code}\n\n` +
      'It expires in 10 minutes. If you did not try to sign in, change your password and ignore this email.',
  };
  await deliver(message, { code });
}

function resetLink(token) {
  const base = (process.env.APP_URL || 'http://localhost:5000').replace(/\/+$/, '');
  return `${base}/api/auth/reset-password?token=${encodeURIComponent(token)}`;
}

async function sendPasswordResetEmail({ to, fullName, token }) {
  const link = resetLink(token);
  const message = {
    to,
    from: fromAddress(),
    subject: 'Reset your Campus Connect password',
    text:
      `Hi ${fullName},\n\n` +
      `Use this link to choose a new password:\n${link}\n\n` +
      'The link can be used once and expires in 30 minutes. If you did not ask for a reset, ignore this email; your password has not changed.',
  };
  await deliver(message, { link });
}

module.exports = {
  sendVerificationEmail,
  sendStaffLoginCode,
  sendPasswordResetEmail,
  verificationLink,
  resetLink,
  outbox,
};
