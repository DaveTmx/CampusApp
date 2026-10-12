// Students: roster list/search (what the Android roster calls), create,
// atomic group assignment, version-checked edits, idempotent soft delete,
// and the student -> lecturer "change request" queue.
//  - assignToGroupTx locks the group row first, so two simultaneous requests
//    for the last place can never both succeed.
//  - editStudent rejects a stale baseVersion with 409 instead of overwriting.
//  - deleteStudent is a soft delete and is safe to repeat.
const crypto = require('crypto');
const pool = require('../config/db');
const { escapeLike, parsePaging, withTransaction } = require('../utils/sql');

const GROUPS = ['G01', 'G02', 'G03', 'G04'];
const PROGRAMMES = ['CS', 'IT', 'DS'];
const NUMBER_RE = /^\d{9}$/;
const FIELD_LABELS = { name: 'Name', group: 'Group', number: 'Student Number' };

// Shape the Android StudentPageDto.StudentDto expects (snake_case).
function toApi(row) {
  return {
    student_id: row.student_id,
    student_number: row.student_number,
    full_name: row.student_name,
    programme: row.programme_code,
    confirmed_group: row.group_code,
    version: row.version,
  };
}

function toPendingApi(row) {
  return {
    id: row.id,
    studentId: row.student_id,
    studentName: row.student_name,
    field: row.field,
    fieldLabel: FIELD_LABELS[row.field],
    newValue: row.new_value,
    status: row.status,
  };
}

function isValidName(name) {
  return typeof name === 'string' && name.trim().length >= 2 && name.trim().length <= 100;
}

// ---------------------------------------------------------------- group assignment

// Exported so sync.routes (the idempotent /api/sync wrapper) can run it INSIDE an
// operation's transaction, and so the plain route below can call it directly.
async function assignToGroupTx(conn, studentId, groupCode) {
  // Lock the group's own row first. Every concurrent request for this
  // group queues on this lock, so the count read right after it is
  // always current -- no other transaction can be mid-assignment for
  // this group while we hold it.
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

// POST /api/students/:id/assign  { groupCode }   (lecturer)
// Run 2 of these at once for the same groupCode: exactly one succeeds.
exports.assignToGroup = async (req, res) => {
  const { groupCode } = req.body;
  if (!GROUPS.includes(groupCode)) {
    return res.status(400).json({ success: false, message: 'groupCode must be G01-G04' });
  }
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const result = await assignToGroupTx(conn, req.params.id, groupCode);
    if (result.error) {
      await conn.rollback();
      const status = result.error === 'GROUP_FULL' ? 409 : result.error === 'STUDENT_NOT_FOUND' ? 404 : 400;
      return res.status(status).json({ success: false, error: result.error });
    }
    await conn.commit();
    res.json({ success: true, groupCode });
  } catch (err) {
    await conn.rollback();
    console.error('assignToGroup failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    conn.release();
  }
};

// ---------------------------------------------------------------- read

// GET /api/students?q=&page=1&pageSize=20[&group=G01|UNASSIGNED]   (lecturer)
// Returns { students: [...], page, hasMore } -- the StudentPageDto shape.
exports.listStudents = async (req, res) => {
  const q = String(req.query.q || '').trim();
  const group = String(req.query.group || '').trim().toUpperCase();
  const { page, pageSize, offset } = parsePaging(req.query);

  const where = ['deleted_at IS NULL'];
  const params = [];
  if (q) {
    const like = `%${escapeLike(q)}%`;
    where.push('(student_name LIKE ? OR student_number LIKE ?)');
    params.push(like, like);
  }
  if (GROUPS.includes(group)) {
    where.push('group_code = ?');
    params.push(group);
  } else if (group === 'UNASSIGNED') {
    where.push('group_code IS NULL');
  }

  try {
    // Fetch one extra row to learn whether another page exists.
    const [rows] = await pool.query(
      `SELECT student_id, student_number, student_name, programme_code, group_code, version
         FROM students
        WHERE ${where.join(' AND ')}
        ORDER BY student_name, student_id
        LIMIT ? OFFSET ?`,
      [...params, pageSize + 1, offset]
    );
    res.json({
      students: rows.slice(0, pageSize).map(toApi),
      page,
      hasMore: rows.length > pageSize,
    });
  } catch (err) {
    console.error('listStudents failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching students' });
  }
};

// GET /api/students/me   (the logged-in student's own record)
exports.getMyStudent = async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT student_id, student_number, student_name, programme_code, group_code, version
         FROM students WHERE account_id = ? AND deleted_at IS NULL`,
      [req.user.id]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, error: 'STUDENT_NOT_FOUND' });
    res.json({ success: true, student: toApi(rows[0]) });
  } catch (err) {
    console.error('getMyStudent failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// GET /api/students/:id   (lecturer, or the student who owns the record)
exports.getStudent = async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT student_id, student_number, student_name, programme_code, group_code, version, account_id
         FROM students WHERE student_id = ? AND deleted_at IS NULL`,
      [req.params.id]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, error: 'STUDENT_NOT_FOUND' });
    if (req.user.role !== 'lecturer' && rows[0].account_id !== req.user.id) {
      return res.status(403).json({ success: false, message: 'You can only view your own record' });
    }
    res.json({ success: true, student: toApi(rows[0]) });
  } catch (err) {
    console.error('getStudent failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// ---------------------------------------------------------------- create / edit / delete

// POST /api/students  { studentName, studentNumber, programmeCode, groupCode? }   (lecturer)
exports.createStudent = async (req, res) => {
  const { studentName, studentNumber, programmeCode, groupCode } = req.body;
  if (!isValidName(studentName)) {
    return res.status(400).json({ success: false, message: 'studentName must be 2-100 characters' });
  }
  if (!NUMBER_RE.test(String(studentNumber || ''))) {
    return res.status(400).json({ success: false, message: 'studentNumber must be exactly 9 digits' });
  }
  if (!PROGRAMMES.includes(programmeCode)) {
    return res.status(400).json({ success: false, message: 'programmeCode must be CS, IT or DS' });
  }
  if (groupCode != null && !GROUPS.includes(groupCode)) {
    return res.status(400).json({ success: false, message: 'groupCode must be G01-G04' });
  }

  const studentId = crypto.randomUUID();
  try {
    const outcome = await withTransaction(pool, async (conn) => {
      await conn.query(
        `INSERT INTO students (student_id, student_name, student_number, programme_code)
         VALUES (?, ?, ?, ?)`,
        [studentId, studentName.trim(), String(studentNumber), programmeCode]
      );
      if (groupCode) {
        const result = await assignToGroupTx(conn, studentId, groupCode);
        if (result.error) throw Object.assign(new Error(result.error), { business: result.error });
      }
      return { ok: true };
    });
    res.status(201).json({ success: true, studentId, version: groupCode ? 2 : 1, ...outcome });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ success: false, error: 'STUDENT_NUMBER_TAKEN' });
    }
    if (err.business) {
      return res.status(err.business === 'GROUP_FULL' ? 409 : 400).json({ success: false, error: err.business });
    }
    console.error('createStudent failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// PUT /api/students/:id  { baseVersion, studentName?, programmeCode?, studentNumber? }
// Rejects a stale edit instead of overwriting. A student may only edit their
// own record, and may not change the student number (that goes through a
// change request a lecturer approves).
exports.editStudent = async (req, res) => {
  const { baseVersion, studentName, programmeCode, studentNumber } = req.body;
  if (!Number.isInteger(baseVersion)) {
    return res.status(400).json({ success: false, message: 'baseVersion (integer) is required' });
  }
  if (studentName !== undefined && !isValidName(studentName)) {
    return res.status(400).json({ success: false, message: 'studentName must be 2-100 characters' });
  }
  if (programmeCode !== undefined && !PROGRAMMES.includes(programmeCode)) {
    return res.status(400).json({ success: false, message: 'programmeCode must be CS, IT or DS' });
  }
  const isLecturer = req.user.role === 'lecturer';
  if (studentNumber !== undefined) {
    if (!isLecturer) {
      return res.status(403).json({ success: false, message: 'Only a lecturer can change a student number' });
    }
    if (!NUMBER_RE.test(String(studentNumber))) {
      return res.status(400).json({ success: false, message: 'studentNumber must be exactly 9 digits' });
    }
  }

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[current]] = await conn.query('SELECT * FROM students WHERE student_id = ? FOR UPDATE', [req.params.id]);
    if (!current) { await conn.rollback(); return res.status(404).json({ success: false, error: 'STUDENT_NOT_FOUND' }); }
    if (!isLecturer && current.account_id !== req.user.id) {
      await conn.rollback();
      return res.status(403).json({ success: false, message: 'You can only edit your own record' });
    }
    if (current.deleted_at) {
      // A stale offline edit must never resurrect a deleted student.
      await conn.rollback();
      return res.status(410).json({ success: false, error: 'STUDENT_DELETED' });
    }
    if (current.version !== baseVersion) {
      await conn.rollback();
      return res.status(409).json({
        success: false,
        error: 'CONFLICT',
        currentRecord: {
          studentId: current.student_id,
          studentName: current.student_name,
          programme: current.programme_code,
          groupCode: current.group_code,
          version: current.version,
        },
      });
    }
    await conn.query(
      `UPDATE students
          SET student_name = ?, programme_code = ?, student_number = ?, version = version + 1
        WHERE student_id = ? AND version = ?`,
      [
        studentName !== undefined ? studentName.trim() : current.student_name,
        programmeCode ?? current.programme_code,
        studentNumber !== undefined ? String(studentNumber) : current.student_number,
        req.params.id,
        baseVersion,
      ]
    );
    await conn.commit();
    res.json({ success: true, version: baseVersion + 1 });
  } catch (err) {
    await conn.rollback();
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ success: false, error: 'STUDENT_NUMBER_TAKEN' });
    }
    console.error('editStudent failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    conn.release();
  }
};

// DELETE /api/students/:id   (lecturer)
// Idempotent soft delete. student_number is never cleared, so it stays reserved.
exports.deleteStudent = async (req, res) => {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[s]] = await conn.query('SELECT deleted_at, group_code FROM students WHERE student_id = ? FOR UPDATE', [req.params.id]);
    if (!s) { await conn.rollback(); return res.status(404).json({ success: false, error: 'STUDENT_NOT_FOUND' }); }
    if (s.deleted_at) { await conn.rollback(); return res.json({ success: true, alreadyDeleted: true }); }
    await conn.query(
      'UPDATE students SET deleted_at = NOW(), group_code = NULL, version = version + 1 WHERE student_id = ?',
      [req.params.id]
    );
    await conn.commit();
    res.json({ success: true, releasedGroup: s.group_code });
  } catch (err) {
    await conn.rollback();
    console.error('deleteStudent failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    conn.release();
  }
};

// ---------------------------------------------------------------- change requests

// POST /api/sync/pending  { field: 'name'|'group'|'number', newValue }   (student)
exports.createPendingChange = async (req, res) => {
  const { field, newValue } = req.body;
  if (!FIELD_LABELS[field]) {
    return res.status(400).json({ success: false, message: 'field must be name, group or number' });
  }
  const value = typeof newValue === 'string' ? newValue.trim() : '';
  if (field === 'name' && !isValidName(value)) {
    return res.status(400).json({ success: false, message: 'Name must be 2-100 characters' });
  }
  if (field === 'group' && !GROUPS.includes(value)) {
    return res.status(400).json({ success: false, message: 'Group must be G01-G04' });
  }
  if (field === 'number' && !NUMBER_RE.test(value)) {
    return res.status(400).json({ success: false, message: 'Student number must be exactly 9 digits' });
  }

  try {
    const [rows] = await pool.query(
      'SELECT student_id, student_name FROM students WHERE account_id = ? AND deleted_at IS NULL',
      [req.user.id]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, error: 'STUDENT_NOT_FOUND' });

    const [result] = await pool.query(
      'INSERT INTO pending_changes (student_id, student_name, field, new_value) VALUES (?, ?, ?, ?)',
      [rows[0].student_id, rows[0].student_name, field, value]
    );
    res.status(201).json({ success: true, id: result.insertId });
  } catch (err) {
    console.error('createPendingChange failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// GET /api/sync/pending   (lecturer)
exports.listPendingChanges = async (req, res) => {
  try {
    const [rows] = await pool.query(
      "SELECT * FROM pending_changes WHERE status = 'Pending' ORDER BY id"
    );
    res.json({ success: true, data: rows.map(toPendingApi) });
  } catch (err) {
    console.error('listPendingChanges failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// Applies or discards one request inside its own transaction.
// Returns { status: <http>, body } so single and bulk callers can share it.
async function resolveOne(id, approve, lecturerId) {
  return withTransaction(pool, async (conn) => {
    const [[change]] = await conn.query('SELECT * FROM pending_changes WHERE id = ? FOR UPDATE', [id]);
    if (!change) return { status: 404, body: { success: false, error: 'REQUEST_NOT_FOUND' } };
    if (change.status !== 'Pending') {
      return { status: 200, body: { success: true, alreadyResolved: true, resolution: change.status } };
    }

    if (approve) {
      if (change.field === 'group') {
        const result = await assignToGroupTx(conn, change.student_id, change.new_value);
        if (result.error) {
          // Leave it Pending so the lecturer can free a place and try again.
          return { status: result.error === 'GROUP_FULL' ? 409 : 400, body: { success: false, error: result.error } };
        }
      } else if (change.field === 'name') {
        await conn.query(
          'UPDATE students SET student_name = ?, version = version + 1 WHERE student_id = ? AND deleted_at IS NULL',
          [change.new_value, change.student_id]
        );
      } else if (change.field === 'number') {
        try {
          await conn.query(
            'UPDATE students SET student_number = ?, version = version + 1 WHERE student_id = ? AND deleted_at IS NULL',
            [change.new_value, change.student_id]
          );
        } catch (err) {
          if (err.code === 'ER_DUP_ENTRY') {
            return { status: 409, body: { success: false, error: 'STUDENT_NUMBER_TAKEN' } };
          }
          throw err;
        }
      }
    }

    await conn.query(
      'UPDATE pending_changes SET status = ?, resolved_at = NOW(), resolved_by = ? WHERE id = ?',
      [approve ? 'Approved' : 'Rejected', lecturerId, id]
    );
    return { status: 200, body: { success: true, resolution: approve ? 'Approved' : 'Rejected' } };
  });
}

// POST /api/sync/pending/:id/approve   (lecturer)
exports.approvePendingChange = async (req, res) => {
  try {
    const { status, body } = await resolveOne(req.params.id, true, req.user.id);
    res.status(status).json(body);
  } catch (err) {
    console.error('approvePendingChange failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// POST /api/sync/pending/:id/reject   (lecturer)
exports.rejectPendingChange = async (req, res) => {
  try {
    const { status, body } = await resolveOne(req.params.id, false, req.user.id);
    res.status(status).json(body);
  } catch (err) {
    console.error('rejectPendingChange failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// POST /api/sync/approve-all   (lecturer: the "Sync now" button)
exports.approveAllPendingChanges = async (req, res) => {
  try {
    const [rows] = await pool.query("SELECT id FROM pending_changes WHERE status = 'Pending' ORDER BY id");
    let approved = 0;
    const failed = [];
    for (const { id } of rows) {
      const { status, body } = await resolveOne(id, true, req.user.id);
      if (status === 200) approved += 1;
      else failed.push({ id, error: body.error });
    }
    res.json({ success: true, approved, failed });
  } catch (err) {
    console.error('approveAllPendingChanges failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

module.exports.assignToGroupTx = assignToGroupTx;
