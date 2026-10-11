// New controller -- the repo currently has no students table, routes, or
// controller at all, despite the lab requiring full student CRUD. This
// file covers the three pieces Sikumbuzo's challenges need: atomic group
// assignment (Challenge 1), version-checked edits and soft delete
// (Challenge 3). Challenge 2's idempotency wrapper lives in
// operationsController.js and calls assignToGroupTx below directly.
const pool = require('../db/pool');

const GROUPS = ['G01', 'G02', 'G03', 'G04'];

// Exported so operationsController.js can run it INSIDE an operation's
// transaction for Challenge 2, and so studentsController's own route can
// call it directly for a non-retried assignment.
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

// POST /api/students/:id/assign  { groupCode }
// Challenge 1's endpoint: run 2 of these at once for the same groupCode,
// exactly one should succeed.
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

// PUT /api/students/:id  { baseVersion, studentName?, programmeCode? }
// Challenge 3, part 1: reject a stale edit instead of overwriting.
exports.editStudent = async (req, res) => {
  const { baseVersion, studentName, programmeCode } = req.body;
  if (!Number.isInteger(baseVersion)) {
    return res.status(400).json({ success: false, message: 'baseVersion (integer) is required' });
  }
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[current]] = await conn.query('SELECT * FROM students WHERE student_id = ? FOR UPDATE', [req.params.id]);
    if (!current) { await conn.rollback(); return res.status(404).json({ success: false, error: 'STUDENT_NOT_FOUND' }); }
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
      'UPDATE students SET student_name = ?, programme_code = ?, version = version + 1 WHERE student_id = ? AND version = ?',
      [studentName ?? current.student_name, programmeCode ?? current.programme_code, req.params.id, baseVersion]
    );
    await conn.commit();
    res.json({ success: true, version: baseVersion + 1 });
  } catch (err) {
    await conn.rollback();
    console.error('editStudent failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    conn.release();
  }
};

// DELETE /api/students/:id
// Challenge 3, part 2: idempotent soft delete. student_number is never
// cleared, so it stays reserved.
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

module.exports.assignToGroupTx = assignToGroupTx;
