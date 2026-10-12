// Idempotency wrapper for offline/retried mutations. The receipt (status +
// result) is written in the SAME transaction as the mutation, so they always
// commit together: a crash or dropped response between "change committed"
// and "client sees the reply" can never duplicate the change, because the
// retry finds the receipt and replays it instead of re-running the handler.
const pool = require('../config/db');
const { assignToGroupTx } = require('../contollers/students.controller');

const HANDLERS = {
  ASSIGN_GROUP: (conn, payload) => assignToGroupTx(conn, payload.studentId, payload.groupCode),
  DELETE_STUDENT: async (conn, payload) => {
    const [[s]] = await conn.query('SELECT deleted_at FROM students WHERE student_id = ? FOR UPDATE', [payload.studentId]);
    if (!s) return { error: 'STUDENT_NOT_FOUND' };
    if (s.deleted_at) return { ok: true, alreadyDeleted: true };
    await conn.query(
      'UPDATE students SET deleted_at = NOW(), group_code = NULL, version = version + 1 WHERE student_id = ?',
      [payload.studentId]
    );
    return { ok: true };
  },
};

// Looks up an existing receipt. Returns an { httpStatus, body } replay, or null.
async function findReceipt(conn, operationId, payload) {
  const [[existing]] = await conn.query(
    'SELECT status, payload_json, result_json FROM operations WHERE operation_id = ?', [operationId]
  );
  if (!existing) return null;
  const same = JSON.stringify(existing.payload_json) === JSON.stringify(payload);
  if (!same) {
    return { httpStatus: 409, body: { success: false, error: 'OPERATION_ID_REUSED_WITH_DIFFERENT_CONTENT' } };
  }
  return { httpStatus: 200, body: { success: true, status: existing.status, result: existing.result_json, replayed: true } };
}

/**
 * Runs (or replays) one operation.
 * input: { operationId, opType, payload, accountId }
 * returns { httpStatus, body }
 */
async function executeOperation({ operationId, opType, payload, accountId }) {
  if (!operationId || !HANDLERS[opType] || !payload || typeof payload !== 'object') {
    return { httpStatus: 400, body: { success: false, message: 'operationId, a known opType, and payload are required' } };
  }

  const conn = await pool.getConnection();
  try {
    const replay = await findReceipt(conn, operationId, payload);
    if (replay) return replay;

    await conn.beginTransaction();
    const result = await HANDLERS[opType](conn, payload);
    const status = result.error ? 'FAILED' : 'APPLIED';
    await conn.query(
      `INSERT INTO operations (operation_id, account_id, student_id, op_type, payload_json, status, result_json)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [operationId, accountId, payload.studentId || null, opType, JSON.stringify(payload), status, JSON.stringify(result)]
    );
    await conn.commit();
    return { httpStatus: result.error ? 409 : 200, body: { success: !result.error, status, result, replayed: false } };
  } catch (err) {
    await conn.rollback();
    if (err.code === 'ER_DUP_ENTRY') {
      // Two retries of the same operationId raced; the other one won. Replay its receipt.
      const replay = await findReceipt(conn, operationId, payload);
      if (replay) return replay;
    }
    console.error('executeOperation failed', err.code || err.message);
    return { httpStatus: 500, body: { success: false, message: 'Server error' } };
  } finally {
    conn.release(); // exactly once, on every path
  }
}

module.exports = { executeOperation, HANDLERS };
