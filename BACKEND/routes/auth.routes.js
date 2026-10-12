const express = require('express');
const router = express.Router();
const authController = require('../contollers/auth.controller');

// POST /api/auth/register
router.post('/register', authController.register);

// POST /api/auth/login
router.post('/login', authController.login);

// POST /api/auth/forgot-password   { email }
router.post('/forgot-password', authController.forgotPassword);

// POST /api/auth/reset-password    { email, otp, newPassword }
router.post('/reset-password', authController.resetPassword);

module.exports = router;
