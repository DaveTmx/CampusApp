// New controller -- Challenge 2's idempotency wrapper. The receipt
// (status + result) is written into the SAME transaction as the actual
// mutation, so they always commit together: a crash or dropped response
// between "change committed" and "client sees the reply" can never
// duplicate the change, because the retry finds the receipt and replays
// it instead of re-running the handler.
const pool = require('../db/pool');
const { assignToGroupTx } = require('./studentsController');

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

// POST /api/operations  { operationId, opType, payload }
exports.submitOperation = async (req, res) => {
  const { operationId, opType, payload } = req.body;
  if (!operationId || !HANDLERS[opType] || typeof payload !== 'object') {
    return res.status(400).json({ success: false, message: 'operationId, a known opType, and payload are required' });
  }
  const accountId = req.user ? req.user.id : null;

  const conn = await pool.getConnection();
  try {
    const [[existing]] = await conn.query(
      'SELECT status, payload_json, result_json FROM operations WHERE operation_id = ?', [operationId]
    );
    if (existing) {
      const same = JSON.stringify(existing.payload_json) === JSON.stringify(payload);
      conn.release();
      if (!same) return res.status(409).json({ success: false, error: 'OPERATION_ID_REUSED_WITH_DIFFERENT_CONTENT' });
      return res.status(200).json({ success: true, status: existing.status, result: existing.result_json, replayed: true });
    }

    await conn.beginTransaction();
    const result = await HANDLERS[opType](conn, payload);
    const status = result.error ? 'FAILED' : 'APPLIED';
    await conn.query(
      `INSERT INTO operations (operation_id, account_id, student_id, op_type, payload_json, status, result_json)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [operationId, accountId, payload.studentId || null, opType, JSON.stringify(payload), status, JSON.stringify(result)]
    );
    await conn.commit();
    res.status(result.error ? 409 : 200).json({ success: !result.error, status, result, replayed: false });
  } catch (err) {
    await conn.rollback();
    console.error('submitOperation failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    conn.release();
  }
};
