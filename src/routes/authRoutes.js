const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const staffAuthController = require('../controllers/staffAuthController');
const { requireAuth } = require('../middleware/auth');
const { signInLimiter, staffCodeLimiter, mailLimiter } = require('../middleware/rateLimit');

// POST /api/auth/register                 create an inactive student account and send a link
router.post('/register', mailLimiter, authController.register);

// GET  /api/auth/verify?token=...         activate the account (link from the email)
router.get('/verify', authController.verifyEmail);

// POST /api/auth/resend-verification      request a new link (generic reply, 60 s cooldown)
router.post('/resend-verification', mailLimiter, authController.resendVerification);

// POST /api/auth/forgot-password          request a reset link (generic reply, sent only to active accounts)
router.post('/forgot-password', mailLimiter, authController.forgotPassword);

// GET  /api/auth/reset-password?token=... page opened from the reset email
router.get('/reset-password', authController.resetPasswordPage);

// POST /api/auth/reset-password           set a new password with a valid link; signs out all sessions
router.post('/reset-password', signInLimiter, authController.resetPassword);

// POST /api/auth/login                    returns a 12-hour JWT
router.post('/login', signInLimiter, authController.login);

// POST /api/auth/staff/login              lecturers: password, then a code is emailed (202)
router.post('/staff/login', staffCodeLimiter, staffAuthController.staffLogin);

// POST /api/auth/staff/verify-code        lecturers: exchange the emailed code for a 12-hour JWT
router.post('/staff/verify-code', staffCodeLimiter, staffAuthController.staffVerifyCode);

// POST /api/auth/logout                   stateless: the client deletes its token
router.post('/logout', requireAuth, authController.logout);

module.exports = router;
