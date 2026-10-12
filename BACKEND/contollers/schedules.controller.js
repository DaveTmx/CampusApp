// Weekly timetable (Mon-Fri). Matches the Android ClassSession:
// { name, time: "09:00 - 10:30", room, type } plus id and groupCode.
// A student sees classes for everyone (group_code NULL) plus their own group;
// a lecturer sees everything (optionally ?group=G01).
const pool = require('../config/db');

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];
const TYPES = ['Lecture', 'Practical', 'Tutorial'];
const GROUPS = ['G01', 'G02', 'G03', 'G04'];
const TIME_RE = /^([01]\d|2[0-3]):[0-5]\d$/;

const COLUMNS = `id, day_index, name, room, session_type, group_code,
                 TIME_FORMAT(start_time, '%H:%i') AS start_hm, TIME_FORMAT(end_time, '%H:%i') AS end_hm`;

function toApi(row) {
  return {
    id: row.id,
    name: row.name,
    time: `${row.start_hm} - ${row.end_hm}`,
    room: row.room,
    type: row.session_type,
    groupCode: row.group_code,
  };
}

// GET /api/schedules?day=0..4   (omit day for the whole week)
exports.getSchedule = async (req, res) => {
  const dayParam = req.query.day;
  let day = null;
  if (dayParam !== undefined) {
    day = parseInt(dayParam, 10);
    if (!Number.isInteger(day) || day < 0 || day > 4) {
      return res.status(400).json({ success: false, message: 'day must be 0 (Mon) to 4 (Fri)' });
    }
  }

  try {
    const where = [];
    const params = [];
    if (day !== null) { where.push('day_index = ?'); params.push(day); }

    if (req.user.role === 'lecturer') {
      const group = String(req.query.group || '').toUpperCase();
      if (GROUPS.includes(group)) { where.push('(group_code IS NULL OR group_code = ?)'); params.push(group); }
    } else {
      const [own] = await pool.query(
        'SELECT group_code FROM students WHERE account_id = ? AND deleted_at IS NULL', [req.user.id]
      );
      const myGroup = own.length > 0 ? own[0].group_code : null;
      where.push('(group_code IS NULL OR group_code = ?)');
      params.push(myGroup);
    }

    const [rows] = await pool.query(
      `SELECT ${COLUMNS} FROM class_sessions
        ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
        ORDER BY day_index, start_time`,
      params
    );

    const days = DAYS.map((label, dayIndex) => ({
      day: label,
      dayIndex,
      sessions: rows.filter((r) => r.day_index === dayIndex).map(toApi),
    })).filter((d) => day === null || d.dayIndex === day);

    res.json({ success: true, days });
  } catch (err) {
    console.error('getSchedule failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching schedule' });
  }
};

// Shared validation for create/update. Returns { error } or { value }.
function parseSession(body) {
  const { dayIndex, name, startTime, endTime, room, type, groupCode } = body;
  if (!Number.isInteger(dayIndex) || dayIndex < 0 || dayIndex > 4) return { error: 'dayIndex must be 0 (Mon) to 4 (Fri)' };
  if (typeof name !== 'string' || !name.trim() || name.length > 100) return { error: 'name is required (max 100 characters)' };
  if (!TIME_RE.test(String(startTime)) || !TIME_RE.test(String(endTime))) return { error: 'startTime and endTime must be HH:MM' };
  if (startTime >= endTime) return { error: 'endTime must be after startTime' };
  if (typeof room !== 'string' || !room.trim() || room.length > 50) return { error: 'room is required (max 50 characters)' };
  if (!TYPES.includes(type)) return { error: 'type must be Lecture, Practical or Tutorial' };
  if (groupCode != null && !GROUPS.includes(groupCode)) return { error: 'groupCode must be G01-G04 or null' };
  return { value: [dayIndex, name.trim(), startTime, endTime, room.trim(), type, groupCode ?? null] };
}

// POST /api/schedules  { dayIndex, name, startTime, endTime, room, type, groupCode? }   (lecturer)
exports.createSession = async (req, res) => {
  const parsed = parseSession(req.body);
  if (parsed.error) return res.status(400).json({ success: false, message: parsed.error });
  try {
    const [result] = await pool.query(
      `INSERT INTO class_sessions (day_index, name, start_time, end_time, room, session_type, group_code)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      parsed.value
    );
    res.status(201).json({ success: true, id: result.insertId });
  } catch (err) {
    console.error('createSession failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error creating class' });
  }
};

// PUT /api/schedules/:id   (lecturer; same body as create)
exports.updateSession = async (req, res) => {
  const parsed = parseSession(req.body);
  if (parsed.error) return res.status(400).json({ success: false, message: parsed.error });
  try {
    const [result] = await pool.query(
      `UPDATE class_sessions
          SET day_index = ?, name = ?, start_time = ?, end_time = ?, room = ?, session_type = ?, group_code = ?
        WHERE id = ?`,
      [...parsed.value, req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, error: 'SESSION_NOT_FOUND' });
    res.json({ success: true });
  } catch (err) {
    console.error('updateSession failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error updating class' });
  }
};

// DELETE /api/schedules/:id   (lecturer)
exports.deleteSession = async (req, res) => {
  try {
    const [result] = await pool.query('DELETE FROM class_sessions WHERE id = ?', [req.params.id]);
    if (result.affectedRows === 0) return res.status(404).json({ success: false, error: 'SESSION_NOT_FOUND' });
    res.json({ success: true });
  } catch (err) {
    console.error('deleteSession failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error deleting class' });
  }
};
