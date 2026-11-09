'use strict';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function normalizeEmail(value) {
  return typeof value === 'string' ? value.trim().toLowerCase() : '';
}

function isValidEmail(email) {
  return email.length > 0 && email.length <= 255 && EMAIL_RE.test(email);
}

// Trimmed string, or null when the value is missing, blank, not a string, or too long.
function cleanText(value, max) {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 && trimmed.length <= max ? trimmed : null;
}

// Parses a query-string integer. Returns `def` when absent, the number when
// valid, or null when present but out of range or not a whole number.
function parseIntParam(value, { def, min, max }) {
  if (value === undefined || value === '') return def;
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null;
  const n = Number(value);
  return n >= min && n <= max ? n : null;
}

// Escapes LIKE wildcards so user search text is matched literally.
function escapeLike(text) {
  return text.replace(/[\\%_]/g, '\\$&');
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

module.exports = { normalizeEmail, isValidEmail, cleanText, parseIntParam, escapeLike, isPlainObject };
