'use strict';

// Student records: lecturer roster, create, read, edit (student or lecturer),
// group assignment and soft delete.
//
// Concurrency rules:
// - Group capacity is enforced under SELECT ... FOR UPDATE on the lab_groups row.
// - Edits use `baseVersion` (optimistic). A stale version gets 409 CONFLICT.
// - Deleted students cannot be edited (410). Deletes are idempotent.
const { randomUUID } = require('crypto');
const pool = require('../db/pool');
const { inTransaction } = require('../db/tx');
const { GROUPS, PROGRAMMES, ROLES } = require('../constants');
const { sendError } = require('../util/http');
const { cleanText, escapeLike, parseIntParam } = require('../util/validation');

const GROUP_ERROR_STATUS = { GROUP_FULL: 409, NO_SUCH_GROUP: 404, STUDENT_NOT_FOUND: 404 };

function upperOrNull(value) {
  return typeof value === 'string' ? value.trim().toUpperCase() : null;
}

// camelCase record: used by single-record responses and conflict replies.
function toRecord(row) {
  return {
    studentId: row.student_id,
    studentNumber: row.student_number,
    studentName: row.student_name,
    programme: row.programme_code,
    groupCode: row.group_code,
    version: row.version,
  };
}

// snake_case item: used by GET /api/students, matching the Android StudentPageDto.
function toListItem(row) {
  return {
    student_id: row.student_id,
    student_number: row.student_number,
    full_name: row.student_name,
    programme: row.programme_code,
    confirmed_group: row.group_code,
    version: row.version,
  };
}

// Runs inside a transaction. Locks the group, checks capacity, then moves the student.
// Returns { ok: true } or { error: 'GROUP_FULL' | 'NO_SUCH_GROUP' | 'STUDENT_NOT_FOUND' }.
async function assignToGroupTx(conn, studentId, groupCode) {
  const [[group]] = await conn.query(
    'SELECT group_code, capacity FROM lab_groups WHERE group_code = ? FOR UPDATE',
    [groupCode]
  );
  if (!group) return { error: 'NO_SUCH_GROUP' };

  const [[{ cnt }]] = await conn.query(
    'SELECT COUNT(*) AS cnt FROM students WHERE group_code = ? AND deleted_at IS NULL',
    [groupCode]
  );
  if (cnt >= group.capacity) return { error: 'GROUP_FULL' };

  const [[student]] = await conn.query(
    'SELECT student_id, deleted_at FROM students WHERE student_id = ? FOR UPDATE',
    [studentId]
  );
  if (!student || student.deleted_at) return { error: 'STUDENT_NOT_FOUND' };

  await conn.query(
    'UPDATE students SET group_code = ?, version = version + 1 WHERE student_id = ?',
    [groupCode, studentId]
  );
  return { ok: true, groupCode };
}

// Runs inside a transaction. Removes the student from their group. Idempotent.
// Returns { ok: true } or { error: 'STUDENT_NOT_FOUND' | 'STUDENT_DELETED' }.
async function unassignTx(conn, studentId) {
  const [[student]] = await conn.query(
    'SELECT student_id, deleted_at FROM students WHERE student_id = ? FOR UPDATE',
    [studentId]
  );
  if (!student) return { error: 'STUDENT_NOT_FOUND' };
  if (student.deleted_at) return { error: 'STUDENT_DELETED' };

  await conn.query(
    'UPDATE students SET group_code = NULL, version = version + 1 WHERE student_id = ? AND group_code IS NOT NULL',
    [studentId]
  );
  return { ok: true };
}

// Runs inside a transaction. Soft-deletes the student, frees their group place,
// and writes a tombstone. Already-deleted students return alreadyDeleted: true.
async function softDeleteTx(conn, studentId, actorId) {
  const [[student]] = await conn.query(
    'SELECT deleted_at, group_code FROM students WHERE student_id = ? FOR UPDATE',
    [studentId]
  );
  if (!student) return { error: 'STUDENT_NOT_FOUND' };
  if (student.deleted_at) return { ok: true, alreadyDeleted: true, releasedGroup: null };

  await conn.query(
    'UPDATE students SET deleted_at = NOW(), group_code = NULL, version = version + 1 WHERE student_id = ?',
    [studentId]
  );
  await conn.query(
    'INSERT IGNORE INTO deletion_markers (student_id, deleted_at, deleted_by) VALUES (?, NOW(), ?)',
    [studentId, actorId ?? null]
  );
  return { ok: true, releasedGroup: student.group_code };
}

// GET /api/students?q=&programme=&group=&unassigned=true&page=1&pageSize=20   (lecturer)
exports.listStudents = async (req, res) => {
  const q = cleanText(req.query.q, 100);
  const page = parseIntParam(req.query.page, { def: 1, min: 1, max: 100000 });
  const pageSize = parseIntParam(req.query.pageSize, { def: 20, min: 1, max: 50 });
  if (page === null || pageSize === null) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'page must be >= 1 and pageSize must be 1-50');
  }

  const programme = req.query.programme === undefined ? null : upperOrNull(req.query.programme);
  if (req.query.programme !== undefined && !PROGRAMMES.includes(programme)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Unknown programme');
  }
  const group = req.query.group === undefined ? null : upperOrNull(req.query.group);
  if (req.query.group !== undefined && !GROUPS.includes(group)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'group must be G01-G04');
  }
  const unassigned = req.query.unassigned === 'true';
  if (unassigned && group) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'Use either group or unassigned, not both');
  }

  // Only fixed SQL fragments are concatenated; every user value is a bound parameter.
  const where = ['s.deleted_at IS NULL'];
  const params = [];
  if (q) {
    const like = `%${escapeLike(q)}%`;
    where.push('(s.student_name LIKE ? OR s.student_number LIKE ?)');
    params.push(like, like);
  }
  if (programme) {
    where.push('s.programme_code = ?');
    params.push(programme);
  }
  if (group) {
    where.push('s.group_code = ?');
    params.push(group);
  }
  if (unassigned) where.push('s.group_code IS NULL');

  // Fetch one extra row to learn whether another page exists.
  params.push(pageSize + 1, (page - 1) * pageSize);
  const [rows] = await pool.query(
    `SELECT s.student_id, s.student_number, s.student_name, s.programme_code, s.group_code, s.version
       FROM students s
      WHERE ${where.join(' AND ')}
      ORDER BY s.student_name, s.student_id
      LIMIT ? OFFSET ?`,
    params
  );

  res.json({
    students: rows.slice(0, pageSize).map(toListItem),
    page,
    pageSize,
    hasMore: rows.length > pageSize,
  });
};

// POST /api/students  { studentName, studentNumber, programmeCode, groupCode? }   (lecturer)
exports.createStudent = async (req, res) => {
  const body = req.body || {};
  const studentName = cleanText(body.studentName, 100);
  const studentNumber = typeof body.studentNumber === 'string' ? body.studentNumber.trim() : '';
  const programmeCode = upperOrNull(body.programmeCode);
  const groupCode = body.groupCode === undefined || body.groupCode === null ? null : body.groupCode;

  if (!studentName || !/^\d{9}$/.test(studentNumber) || !PROGRAMMES.includes(programmeCode)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'studentName, a 9-digit studentNumber and a valid programmeCode are required');
  }
  if (groupCode !== null && !GROUPS.includes(groupCode)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'groupCode must be G01-G04');
  }

  const studentId = randomUUID();
  let outcome;
  try {
    outcome = await inTransaction(async (conn) => {
      await conn.query(
        'INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version) VALUES (?, ?, ?, ?, NULL, 1)',
        [studentId, studentName, studentNumber, programmeCode]
      );
      if (groupCode) {
        const placed = await assignToGroupTx(conn, studentId, groupCode);
        if (placed.error) return { rollback: true, error: placed.error }; // no student row without the seat
      }
      return { ok: true };
    });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY' && err.message.includes('uq_student_number')) {
      return sendError(res, 409, 'DUPLICATE_NUMBER', 'A student with this student number already exists');
    }
    throw err;
  }

  if (outcome.error) {
    return sendError(res, GROUP_ERROR_STATUS[outcome.error] ?? 409, outcome.error, 'The group could not take this student');
  }
  res.status(201).json({
    success: true,
    student: { studentId, studentNumber, studentName, programme: programmeCode, groupCode, version: 1 },
  });
};

// GET /api/students/:id   (lecturer: any record; student: own record only)
exports.getStudent = async (req, res) => {
  const [[row]] = await pool.query('SELECT * FROM students WHERE student_id = ?', [req.params.id]);
  if (!row) return sendError(res, 404, 'STUDENT_NOT_FOUND', 'Student not found');
  if (row.deleted_at) return sendError(res, 410, 'STUDENT_DELETED', 'This student has been deleted');
  if (req.user.role === ROLES.STUDENT && row.account_id !== req.user.id) {
    return sendError(res, 403, 'NOT_OWNER', 'You can only view your own record');
  }
  res.json({ success: true, student: toRecord(row) });
};

// PUT /api/students/:id  { baseVersion, studentName?, programmeCode? }
// Students may change their own name only. Lecturers may change name and programme.
exports.editStudent = async (req, res) => {
  try {
    return await applyStudentEdit(req, res);
  } catch (err) {
    // Two edits racing for the same number: the unique index refuses the second.
    if (err.code === 'ER_DUP_ENTRY' && err.message.includes('uq_student_number')) {
      return sendError(res, 409, 'DUPLICATE_NUMBER', 'A student with this student number already exists');
    }
    throw err;
  }
};

async function applyStudentEdit(req, res) {
  const body = req.body || {};
  if (!Number.isInteger(body.baseVersion)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'baseVersion (integer) is required');
  }

  let newName;
  if (body.studentName !== undefined) {
    newName = cleanText(body.studentName, 100);
    if (newName === null) return sendError(res, 400, 'VALIDATION_FAILED', 'studentName must be 1-100 characters');
  }
  let newProgramme;
  if (body.programmeCode !== undefined) {
    newProgramme = upperOrNull(body.programmeCode);
    if (!PROGRAMMES.includes(newProgramme)) return sendError(res, 400, 'VALIDATION_FAILED', 'Unknown programmeCode');
  }
  let newNumber;
  if (body.studentNumber !== undefined) {
    newNumber = typeof body.studentNumber === 'string' ? body.studentNumber.trim() : '';
    if (!/^[0-9]{9}$/.test(newNumber)) {
      return sendError(res, 400, 'VALIDATION_FAILED', 'studentNumber must be exactly 9 digits');
    }
  }
  if (newName === undefined && newProgramme === undefined && newNumber === undefined) {
    return sendError(res, 400, 'NOTHING_TO_UPDATE', 'Send studentName, programmeCode and/or studentNumber');
  }

  const outcome = await inTransaction(async (conn) => {
    const [[current]] = await conn.query('SELECT * FROM students WHERE student_id = ? FOR UPDATE', [req.params.id]);
    if (!current) return { status: 404, error: 'STUDENT_NOT_FOUND', message: 'Student not found' };
    // A stale offline edit must never bring a deleted student back.
    if (current.deleted_at) return { status: 410, error: 'STUDENT_DELETED', message: 'This student has been deleted' };

    if (req.user.role === ROLES.STUDENT) {
      if (current.account_id !== req.user.id) {
        return { status: 403, error: 'NOT_OWNER', message: 'You can only edit your own record' };
      }
      if (newProgramme !== undefined) {
        return { status: 403, error: 'FIELD_NOT_ALLOWED', message: 'Only a lecturer can change the programme' };
      }
      if (newNumber !== undefined) {
        return { status: 403, error: 'FIELD_NOT_ALLOWED', message: 'Only a lecturer can change the student number' };
      }
    }

    if (current.version !== body.baseVersion) {
      return {
        status: 409,
        error: 'CONFLICT',
        message: 'This record was changed by someone else',
        currentRecord: toRecord(current),
      };
    }

    await conn.query(
      'UPDATE students SET student_name = ?, programme_code = ?, student_number = ?, version = version + 1 WHERE student_id = ? AND version = ?',
      [
        newName ?? current.student_name,
        newProgramme ?? current.programme_code,
        newNumber ?? current.student_number,
        current.student_id,
        current.version,
      ]
    );
    return { ok: true, version: current.version + 1 };
  });

  if (outcome.error) {
    return sendError(res, outcome.status, outcome.error, outcome.message,
      outcome.currentRecord ? { currentRecord: outcome.currentRecord } : {});
  }
  res.json({ success: true, version: outcome.version });
}

// POST /api/students/:id/assign  { groupCode }   (lecturer)
exports.assignToGroup = async (req, res) => {
  const groupCode = req.body?.groupCode;
  if (!GROUPS.includes(groupCode)) {
    return sendError(res, 400, 'VALIDATION_FAILED', 'groupCode must be G01-G04');
  }

  const outcome = await inTransaction(async (conn) => {
    const result = await assignToGroupTx(conn, req.params.id, groupCode);
    return result.error ? { rollback: true, error: result.error } : result;
  });

  if (outcome.error) {
    return sendError(res, GROUP_ERROR_STATUS[outcome.error] ?? 409, outcome.error, 'Group assignment was refused');
  }
  res.json({ success: true, groupCode });
};

// POST /api/students/:id/unassign   (lecturer). Idempotent: unassigning an unassigned student succeeds.
exports.unassignFromGroup = async (req, res) => {
  const outcome = await inTransaction(async (conn) => {
    const result = await unassignTx(conn, req.params.id);
    return result.error ? { rollback: true, error: result.error } : result;
  });

  if (outcome.error) {
    return sendError(res, outcome.error === 'STUDENT_DELETED' ? 410 : 404, outcome.error, 'Student not found');
  }
  res.json({ success: true, groupCode: null });
};

// DELETE /api/students/:id   (lecturer). Idempotent.
exports.deleteStudent = async (req, res) => {
  const outcome = await inTransaction(async (conn) => {
    const result = await softDeleteTx(conn, req.params.id, req.user.id);
    return result.error ? { rollback: true, error: result.error } : result;
  });

  if (outcome.error) return sendError(res, 404, outcome.error, 'Student not found');
  if (outcome.alreadyDeleted) return res.json({ success: true, alreadyDeleted: true });
  res.json({ success: true, releasedGroup: outcome.releasedGroup });
};

module.exports = {
  assignToGroupTx,
  softDeleteTx,
  listStudents: exports.listStudents,
  createStudent: exports.createStudent,
  getStudent: exports.getStudent,
  editStudent: exports.editStudent,
  assignToGroup: exports.assignToGroup,
  unassignFromGroup: exports.unassignFromGroup,
  unassignTx,
  deleteStudent: exports.deleteStudent,
};
