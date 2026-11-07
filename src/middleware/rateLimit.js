'use strict';

// Per-IP limits on the sign-in and registration routes. These slow down password
// guessing and code spam from one address. The per-account lockout (5 failures,
// 15 minutes) still applies on top of them.
//
// Behind a reverse proxy, set TRUST_PROXY=1 so the limit sees the client address,
// not the proxy's. Without it, every user shares one address.
const rateLimit = require('express-rate-limit');
const { sendError } = require('../util/http');

const WINDOW_MS = 15 * 60 * 1000;
const IS_TEST = process.env.NODE_ENV === 'test';

function limiter(limit, name) {
  return rateLimit({
    windowMs: WINDOW_MS,
    limit,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    // Tests make many requests from one address, so the limiter is switched off there.
    skip: () => IS_TEST,
    handler: (req, res) => {
      console.warn(`rate limit hit: ${name} from ${req.ip}`);
      sendError(res, 429, 'RATE_LIMITED', 'Too many requests. Wait a few minutes and try again.');
    },
  });
}

// Password and sign-in attempts.
const signInLimiter = limiter(30, 'sign-in');
// Code requests and code guesses: stricter, since each guess is cheap to repeat.
const staffCodeLimiter = limiter(15, 'staff-code');
// Registration and resend emails: each one sends mail, so keep the cap tight.
const mailLimiter = limiter(10, 'mail');

module.exports = { signInLimiter, staffCodeLimiter, mailLimiter };
