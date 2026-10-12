// Notices: lecturers post, everyone reads. `isNew` is per account (true until
// that account opens/marks the notice), which drives the bell badge.
// JSON matches the Android Notice: { id, title, body, category, isNew }.
const pool = require('../config/db');

const CATEGORIES = ['Academic', 'Admin', 'Urgent'];

function toApi(row) {
  return {
    id: Number(row.id),
    title: row.title,
    body: row.body,
    category: row.category,
    isNew: Boolean(row.is_new),
    createdAt: row.created_at,
  };
}

// GET /api/notices   (newest first)
exports.listNotices = async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT n.id, n.title, n.body, n.category, n.created_at, (r.notice_id IS NULL) AS is_new
         FROM notices n
         LEFT JOIN notice_reads r ON r.notice_id = n.id AND r.account_id = ?
        ORDER BY n.created_at DESC, n.id DESC
        LIMIT 100`,
      [req.user.id]
    );
    res.json({ success: true, data: rows.map(toApi), unreadCount: rows.filter((r) => r.is_new).length });
  } catch (err) {
    console.error('listNotices failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching notices' });
  }
};

// POST /api/notices  { title, body, category }   (lecturer)
exports.createNotice = async (req, res) => {
  const title = typeof req.body.title === 'string' ? req.body.title.trim() : '';
  const body = typeof req.body.body === 'string' ? req.body.body.trim() : '';
  const { category } = req.body;
  if (!title || title.length > 150) {
    return res.status(400).json({ success: false, message: 'Title is required (max 150 characters)' });
  }
  if (!body) {
    return res.status(400).json({ success: false, message: 'Body is required' });
  }
  if (!CATEGORIES.includes(category)) {
    return res.status(400).json({ success: false, message: 'category must be Academic, Admin or Urgent' });
  }
  try {
    const [result] = await pool.query(
      'INSERT INTO notices (title, body, category, created_by) VALUES (?, ?, ?, ?)',
      [title, body, category, req.user.id]
    );
    // The author has obviously seen their own notice.
    await pool.query('INSERT IGNORE INTO notice_reads (notice_id, account_id) VALUES (?, ?)', [result.insertId, req.user.id]);
    res.status(201).json({ success: true, id: result.insertId });
  } catch (err) {
    console.error('createNotice failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error creating notice' });
  }
};

// DELETE /api/notices/:id   (lecturer)
exports.deleteNotice = async (req, res) => {
  try {
    const [result] = await pool.query('DELETE FROM notices WHERE id = ?', [req.params.id]);
    if (result.affectedRows === 0) return res.status(404).json({ success: false, error: 'NOTICE_NOT_FOUND' });
    res.json({ success: true });
  } catch (err) {
    console.error('deleteNotice failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error deleting notice' });
  }
};

// POST /api/notices/:id/read
exports.markRead = async (req, res) => {
  try {
    const [[notice]] = await pool.query('SELECT id FROM notices WHERE id = ?', [req.params.id]);
    if (!notice) return res.status(404).json({ success: false, error: 'NOTICE_NOT_FOUND' });
    await pool.query('INSERT IGNORE INTO notice_reads (notice_id, account_id) VALUES (?, ?)', [notice.id, req.user.id]);
    res.json({ success: true });
  } catch (err) {
    console.error('markRead failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};

// POST /api/notices/read-all
exports.markAllRead = async (req, res) => {
  try {
    await pool.query('INSERT IGNORE INTO notice_reads (notice_id, account_id) SELECT id, ? FROM notices', [req.user.id]);
    res.json({ success: true });
  } catch (err) {
    console.error('markAllRead failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error' });
  }
};
