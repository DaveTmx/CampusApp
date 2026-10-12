// Lecturer-facing data: staff directory, own profile, and the group summary
// behind LecturerSummaryActivity (aggregate counts only, no student names).
const pool = require('../config/db');

const CAPACITY_FALLBACK = 15;

// GET /api/lecturers   (any logged-in user: staff directory)
exports.listLecturers = async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT u.id, u.full_name, u.email, l.department, l.office
         FROM users u LEFT JOIN lecturers l ON l.account_id = u.id
        WHERE u.role = 'lecturer'
        ORDER BY u.full_name`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error('listLecturers failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching lecturers' });
  }
};

// GET /api/lecturers/me   (lecturer)
exports.getMyProfile = async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT u.id, u.full_name, u.email, l.department, l.office
         FROM users u LEFT JOIN lecturers l ON l.account_id = u.id
        WHERE u.id = ?`,
      [req.user.id]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Account not found' });
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    console.error('getMyProfile failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// PUT /api/lecturers/me  { department?, office? }   (lecturer)
exports.updateMyProfile = async (req, res) => {
  const { department, office } = req.body;
  if ((department != null && String(department).length > 100) || (office != null && String(office).length > 50)) {
    return res.status(400).json({ success: false, message: 'department is max 100 characters, office max 50' });
  }
  try {
    await pool.query(
      `INSERT INTO lecturers (account_id, department, office) VALUES (?, ?, ?)
       ON DUPLICATE KEY UPDATE department = COALESCE(VALUES(department), department),
                               office = COALESCE(VALUES(office), office)`,
      [req.user.id, department ?? null, office ?? null]
    );
    res.json({ success: true });
  } catch (err) {
    console.error('updateMyProfile failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// GET /api/lecturers/summary   (lecturer)
// { groups: [{ group, count, capacity, isFull }], unassigned, total }
exports.getGroupSummary = async (req, res) => {
  try {
    const [groups] = await pool.query(
      `SELECT g.group_code, g.capacity, COUNT(s.student_id) AS cnt
         FROM lab_groups g
         LEFT JOIN students s ON s.group_code = g.group_code AND s.deleted_at IS NULL
        GROUP BY g.group_code, g.capacity
        ORDER BY g.group_code`
    );
    const [[{ unassigned }]] = await pool.query(
      'SELECT COUNT(*) AS unassigned FROM students WHERE group_code IS NULL AND deleted_at IS NULL'
    );
    const data = groups.map((g) => ({
      group: g.group_code,
      count: Number(g.cnt),
      capacity: g.capacity || CAPACITY_FALLBACK,
      isFull: Number(g.cnt) >= (g.capacity || CAPACITY_FALLBACK),
    }));
    res.json({
      success: true,
      groups: data,
      unassigned: Number(unassigned),
      total: data.reduce((sum, g) => sum + g.count, 0) + Number(unassigned),
    });
  } catch (err) {
    console.error('getGroupSummary failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching summary' });
  }
};
