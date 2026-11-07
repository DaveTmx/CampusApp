'use strict';

// Idempotent lecturer operations (Challenge 2). A client may retry an operation
// with the same operationId, and the server will never apply it twice.
//
// How it works:
// - The receipt (status, HTTP status and result) is written in the same
//   transaction as the change, so they commit or roll back together.
// - The receipt row is locked (FOR UPDATE) before the change is attempted, so two
//   concurrent requests with one operationId are serialised.
// - A retry with the same operationId and identical content replays the stored
//   response with the original HTTP status and replayed: true.
// - The same operationId with different content gets 409 and is not applied.
const pool = require('../db/pool');
const { inTransaction } = require('../db/tx');
const { GROUPS } = require('../constants');
const { sendError } = require('../util/http');
const { canonicalJson } = require('../util/json');
const { isPlainObject } = require('../util/validation');
const { assignToGroupTx, softDeleteTx, unassignTx } = require('./studentsController');

const OPERATION_ID = /^[A-Za-z0-9-]{8,36}$/;
const isStudentId = (value) => typeof value === 'string' && value.length > 0 && value.length <= 36;

const OPERATIONS = {
  ASSIGN_GROUP: {
    validate: (p) => isStudentId(p.studentId) && GROUPS.includes(p.groupCode),
    run: (conn, p) => assignToGroupTx(conn, p.studentId, p.groupCode),
    errorStatus: { GROUP_FULL: 409, NO_SUCH_GROUP: 404, STUDENT_NOT_FOUND: 404 },
  },
  UNASSIGN_GROUP: {
    validate: (p) => isStudentId(p.studentId),
    run: (conn, p) => unassignTx(conn, p.studentId),
    errorStatus: { STUDENT_NOT_FOUND: 404, STUDENT_DELETED: 410 },
  },
  DELETE_STUDENT: {
    validate: (p) => isStudentId(p.studentId),
    run: (conn, p, actorId) => softDeleteTx(conn, p.studentId, actorId),
    errorStatus: { STUDENT_NOT_FOUND: 404 },
  },
};

const parseJson = (value) => (typeof value === 'string' ? JSON.parse(value) : value);

// Replays a stored receipt, or refuses if the operationId was used for different content.
function replyFromReceipt(res, receipt, fingerprint) {
  if (canonicalJson(parseJson(receipt.payload_json)) !== fingerprint) {
    return sendError(res, 409, 'OPERATION_ID_REUSED_WITH_DIFFERENT_CONTENT',
      'This operationId was already used for a different request');
  }
  return res.status(receipt.http_status).json({
    success: receipt.status === 'APPLIED',
    status: receipt.status,
    result: parseJson(receipt.result_json),
    replayed: true,
    operationId: receipt.operation_id,
  });
}

// POST /api/operations  { operationId, opType, payload }   (lecturer)
exports.submitOperation = async (req, res) => {
  const { operationId, opType, payload } = req.body || {};

  if (typeof operationId !== 'string' || !OPERATION_ID.test(operationId)) {
    return sendError(res, 400, 'INVALID_OPERATION_ID', 'operationId must be a UUID or similar 8-36 character id');
  }
  if (typeof opType !== 'string' || !Object.hasOwn(OPERATIONS, opType)) {
    return sendError(res, 400, 'UNKNOWN_OP_TYPE', `opType must be one of ${Object.keys(OPERATIONS).join(', ')}`);
  }
  const op = OPERATIONS[opType];
  if (!isPlainObject(payload) || !op.validate(payload)) {
    return sendError(res, 400, 'INVALID_PAYLOAD', 'payload is missing or invalid for this opType');
  }

  const actorId = req.user.id;
  const fingerprint = canonicalJson({ opType, payload });

  try {
    const outcome = await inTransaction(async (conn) => {
      const [[receipt]] = await conn.query(
        'SELECT operation_id, status, http_status, payload_json, result_json FROM operations WHERE operation_id = ? FOR UPDATE',
        [operationId]
      );
      if (receipt) return { rollback: true, replay: receipt };

      const result = await op.run(conn, payload, actorId);
      const status = result.error ? 'FAILED' : 'APPLIED';
      const httpStatus = result.error ? (op.errorStatus[result.error] ?? 409) : 200;
      await conn.query(
        `INSERT INTO operations (operation_id, account_id, student_id, op_type, payload_json, http_status, status, result_json)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [operationId, actorId, payload.studentId ?? null, opType, fingerprint, httpStatus, status, JSON.stringify(result)]
      );
      return { fresh: { result, status, httpStatus } };
    });

    if (outcome.replay) return replyFromReceipt(res, outcome.replay, fingerprint);

    const { result, status, httpStatus } = outcome.fresh;
    return res.status(httpStatus).json({
      success: status === 'APPLIED',
      status,
      result,
      replayed: false,
      operationId,
      ...(status === 'FAILED' ? { error: result.error } : {}),
    });
  } catch (err) {
    // Backstop: another request with this operationId committed first.
    if (err.code === 'ER_DUP_ENTRY') {
      const [[receipt]] = await pool.query(
        'SELECT operation_id, status, http_status, payload_json, result_json FROM operations WHERE operation_id = ?',
        [operationId]
      );
      if (receipt) return replyFromReceipt(res, receipt, fingerprint);
    }
    throw err;
  }
};
