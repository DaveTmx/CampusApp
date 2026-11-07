'use strict';

// Key-order-independent JSON string. Two requests with the same content produce
// the same fingerprint even if their keys arrive in a different order.
function sortKeys(value) {
  if (Array.isArray(value)) return value.map(sortKeys);
  if (value !== null && typeof value === 'object') {
    const out = {};
    for (const key of Object.keys(value).sort()) out[key] = sortKeys(value[key]);
    return out;
  }
  return value;
}

function canonicalJson(value) {
  return JSON.stringify(sortKeys(value));
}

module.exports = { canonicalJson };
