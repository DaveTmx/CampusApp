// Report an Issue: students report campus problems (broken projector, leaking
// tap...), lecturers/admin triage them. "Submitted: Yesterday • ID: #4402" on
// the Android screen is created_at + id.
const pool = require('../config/db');

const STATUSES = ['Pending', 'In Progress', 'Resolved'];

function toApi(row) {
  return {
    id: Number(row.id),
    category: row.category,
    description: row.description,
    status: row.status,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    reportedBy: row.reporter_name, // only present on the lecturer list
  };
}

// POST /api/issues  { category, description }
exports.createIssue = async (req, res) => {
  const category = typeof req.body.category === 'string' ? req.body.category.trim() : '';
  const description = typeof req.body.description === 'string' ? req.body.description.trim() : '';
  if (!category || category.length > 80) {
    return res.status(400).json({ success: false, message: 'Category is required (max 80 characters)' });
  }
  if (description.length < 5 || description.length > 1000) {
    return res.status(400).json({ success: false, message: 'Description must be 5-1000 characters' });
  }
  try {
    const [result] = await pool.query(
      'INSERT INTO issues (account_id, category, description) VALUES (?, ?, ?)',
      [req.user.id, category, description]
    );
    res.status(201).json({ success: true, id: result.insertId });
  } catch (err) {
    console.error('createIssue failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error submitting issue' });
  }
};

// GET /api/issues/mine   ("Your reports")
exports.listMyIssues = async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, category, description, status, created_at, updated_at FROM issues WHERE account_id = ? ORDER BY id DESC',
      [req.user.id]
    );
    res.json({ success: true, data: rows.map(toApi) });
  } catch (err) {
    console.error('listMyIssues failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching issues' });
  }
};

// GET /api/issues?status=Pending   (lecturer)
exports.listAllIssues = async (req, res) => {
  const { status } = req.query;
  if (status !== undefined && !STATUSES.includes(status)) {
    return res.status(400).json({ success: false, message: 'status must be Pending, In Progress or Resolved' });
  }
  try {
    const [rows] = await pool.query(
      `SELECT i.id, i.category, i.description, i.status, i.created_at, i.updated_at, u.full_name AS reporter_name
         FROM issues i JOIN users u ON u.id = i.account_id
        ${status ? 'WHERE i.status = ?' : ''}
        ORDER BY i.id DESC LIMIT 200`,
      status ? [status] : []
    );
    res.json({ success: true, data: rows.map(toApi) });
  } catch (err) {
    console.error('listAllIssues failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching issues' });
  }
};

// PATCH /api/issues/:id/status  { status }   (lecturer)
exports.updateIssueStatus = async (req, res) => {
  const { status } = req.body;
  if (!STATUSES.includes(status)) {
    return res.status(400).json({ success: false, message: 'status must be Pending, In Progress or Resolved' });
  }
  try {
    const [result] = await pool.query('UPDATE issues SET status = ? WHERE id = ?', [status, req.params.id]);
    if (result.affectedRows === 0) return res.status(404).json({ success: false, error: 'ISSUE_NOT_FOUND' });
    res.json({ success: true, status });
  } catch (err) {
    console.error('updateIssueStatus failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error updating issue' });
  }
};
