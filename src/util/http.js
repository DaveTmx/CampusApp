'use strict';

// Every error response has the same shape: { success: false, error, message }.
// `error` is a stable code the Android client can switch on; `message` is for humans.
function sendError(res, status, error, message, extra = {}) {
  return res.status(status).json({ success: false, error, message, ...extra });
}

module.exports = { sendError };
