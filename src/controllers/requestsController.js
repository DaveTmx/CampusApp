'use strict';

// Student requests: a group change or a student-number correction. A student asks;
// a lecturer approves or rejects. Nothing changes on the record until approval.
const pool = require('../db/pool');
const { inTransaction } = require('../db/tx');
const { GROUPS, ROLES } = require('../constants');
const { sendError } = require('../util/http');
const { assignToGroupTx } = require('./studentsController');

const GROUP_CHANGE = 'GROUP_CHANGE';
const NUMBER_CORRECTION = 'NUMBER_CORRECTION';
const STATUSES = ['PENDING', 'APPROVED', 'REJECTED'];

function toRequest(row) {
  return {
    id: row.id,
    studentId: row.student_id,
    type: row.request_type,
    requestedValue: row.requested_value,
    status: row.status,
    createdAt: row.created_at,
    decidedAt: row.decided_at,
  };
}

// POST /api/students/:id/requests   (student, own record only)
// body: { type: 'GROUP' | 'NUMBER', value }
exports.createRequest = async (req, res) => {
  const body = req.body || {};
  const type = body.type === 'GROUP' ? GROUP_CHANGE : body.type === 'NUMBER' ? NUMBER_CORRECTION : null;
  if (!type) {
    return sendError(res, 400, 'VALIDATION_FAILED', "type must be 'GROUP' or 'NUMBER'");
  }
  const value = typeof body.value === 'string' ? body.value.trim().toUpperCase() : '';
  if (type === GROUP_CHANGE && !GROUPS.includes(value)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'value must be a lab group code (G01-G14)');
  }
  if (type === NUMBER_CORRECTION && !/^[0-9]{9}$/.test(value)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'value must be exactly 9 digits');
  }

  const outcome = await inTransaction(async (conn) => {
    const [[student]] = await conn.query('SELECT * FROM students WHERE student_id = ? FOR UPDATE', [req.params.id]);
    if (!student) return { status: 404, error: 'STUDENT_NOT_FOUND', message: 'Student not found' };
    if (student.deleted_at) return { status: 410, error: 'STUDENT_DELETED', message: 'This student has been deleted' };
    if (student.account_id !== req.user.id) {
      return { status: 403, error: 'NOT_OWNER', message: 'You can only request changes to your own record' };
    }
    if (type === GROUP_CHANGE && value === student.group_code) {
      return { status: 409, error: 'ALREADY_IN_GROUP', message: 'You are already in that group' };
    }
    if (type === NUMBER_CORRECTION && value === student.student_number) {
      return { status: 409, error: 'ALREADY_HAS_NUMBER', message: 'That is already your student number' };
    }
    const [[pending]] = await conn.query(
      "SELECT id FROM student_requests WHERE student_id = ? AND request_type = ? AND status = 'PENDING' LIMIT 1",
      [student.student_id, type]
    );
    if (pending) {
      return { status: 409, error: 'REQUEST_PENDING', message: 'You already have a pending request of this kind' };
    }
    if (type === NUMBER_CORRECTION) {
      const [[taken]] = await conn.query('SELECT student_id FROM students WHERE student_number = ? LIMIT 1', [value]);
      if (taken) return { status: 409, error: 'DUPLICATE_NUMBER', message: 'That student number is already in use' };
    }

    const [inserted] = await conn.query(
      'INSERT INTO student_requests (student_id, request_type, requested_value) VALUES (?, ?, ?)',
      [student.student_id, type, value]
    );
    const [[row]] = await conn.query('SELECT * FROM student_requests WHERE id = ?', [inserted.insertId]);
    return { status: 201, request: toRequest(row) };
  });

  if (outcome.error) return sendError(res, outcome.status, outcome.error, outcome.message);
  return res.status(201).json({ success: true, request: outcome.request });
};

// GET /api/students/:id/requests   (student for own record; lecturer for any)
exports.listForStudent = async (req, res) => {
  const [[student]] = await pool.query('SELECT student_id, account_id FROM students WHERE student_id = ?', [req.params.id]);
  if (!student) return sendError(res, 404, 'STUDENT_NOT_FOUND', 'Student not found');
  if (req.user.role === ROLES.STUDENT && student.account_id !== req.user.id) {
    return sendError(res, 403, 'NOT_OWNER', 'You can only see your own requests');
  }
  const [rows] = await pool.query(
    'SELECT * FROM student_requests WHERE student_id = ? ORDER BY created_at DESC, id DESC LIMIT 50',
    [student.student_id]
  );
  return res.json({ success: true, requests: rows.map(toRequest) });
};

// GET /api/requests?status=PENDING   (lecturer)
exports.listRequests = async (req, res) => {
  const status = req.query.status === undefined ? 'PENDING' : String(req.query.status).toUpperCase();
  if (!STATUSES.includes(status)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'status must be PENDING, APPROVED or REJECTED');
  }
  const [rows] = await pool.query(
    `SELECT r.*, s.student_name, s.student_number, s.group_code
       FROM student_requests r
       JOIN students s ON s.student_id = r.student_id
      WHERE r.status = ?
      ORDER BY r.created_at ASC, r.id ASC
      LIMIT 200`,
    [status]
  );
  return res.json({
    success: true,
    requests: rows.map((r) => ({
      ...toRequest(r),
      studentName: r.student_name,
      studentNumber: r.student_number,
      currentGroup: r.group_code,
    })),
  });
};

// POST /api/requests/:id/approve and /reject   (lecturer)
async function decide(req, res, approve) {
  const outcome = await inTransaction(async (conn) => {
    const [[request]] = await conn.query('SELECT * FROM student_requests WHERE id = ? FOR UPDATE', [req.params.id]);
    if (!request) return { status: 404, error: 'REQUEST_NOT_FOUND', message: 'Request not found' };
    if (request.status !== 'PENDING') {
      return { status: 409, error: 'ALREADY_DECIDED', message: 'This request has already been decided' };
    }

    if (approve) {
      const [[student]] = await conn.query('SELECT student_id, deleted_at FROM students WHERE student_id = ? FOR UPDATE', [request.student_id]);
      if (!student || student.deleted_at) {
        return { status: 410, error: 'STUDENT_DELETED', message: 'This student has been deleted' };
      }
      if (request.request_type === GROUP_CHANGE) {
        const moved = await assignToGroupTx(conn, request.student_id, request.requested_value);
        if (moved.error === 'GROUP_FULL') {
          return { status: 409, error: 'GROUP_FULL', message: 'That group is full' };
        }
        if (moved.error) {
          return { status: 404, error: moved.error, message: 'The group could not be changed' };
        }
      } else {
        const [[taken]] = await conn.query(
          'SELECT student_id FROM students WHERE student_number = ? AND student_id <> ? LIMIT 1',
          [request.requested_value, request.student_id]
        );
        if (taken) return { status: 409, error: 'DUPLICATE_NUMBER', message: 'That student number is already in use' };
        await conn.query(
          'UPDATE students SET student_number = ?, version = version + 1 WHERE student_id = ?',
          [request.requested_value, request.student_id]
        );
      }
    }

    await conn.query(
      'UPDATE student_requests SET status = ?, decided_at = NOW(), decided_by = ? WHERE id = ?',
      [approve ? 'APPROVED' : 'REJECTED', req.user.id, request.id]
    );
    return { status: 200, requestId: request.id, decision: approve ? 'APPROVED' : 'REJECTED' };
  });

  if (outcome.error) return sendError(res, outcome.status, outcome.error, outcome.message);
  return res.json({ success: true, requestId: outcome.requestId, status: outcome.decision });
}

exports.approve = (req, res) => decide(req, res, true);
exports.reject = (req, res) => decide(req, res, false);
