// Group chat: a message is visible only to members of that lab group
// (and to lecturers). Matches the Android ChatMessage shape:
// { id, group, senderName, text, timestamp (epoch ms) }.
const pool = require('../config/db');

const MAX_LENGTH = 1000;

// Returns the group the caller may chat in, or null.
// Students: their own group_code. Lecturers: any valid group.
async function canAccess(user, groupCode) {
  if (user.role === 'lecturer') {
    const [g] = await pool.query('SELECT group_code FROM lab_groups WHERE group_code = ?', [groupCode]);
    return g.length > 0;
  }
  const [rows] = await pool.query(
    'SELECT group_code FROM students WHERE account_id = ? AND deleted_at IS NULL', [user.id]
  );
  return rows.length > 0 && rows[0].group_code === groupCode;
}

function toApi(row) {
  return {
    id: Number(row.id),
    group: row.group_code,
    senderName: row.sender_name,
    text: row.message_text,
    timestamp: Number(row.ts),
  };
}

const SELECT_MESSAGE =
  `SELECT id, group_code, sender_name, message_text,
          CAST(UNIX_TIMESTAMP(created_at) * 1000 AS UNSIGNED) AS ts
     FROM chat_messages`;

// GET /api/chat/:groupCode/messages?afterId=0&limit=50
// Oldest first. Pass the last id you have as afterId to poll for new ones.
exports.getMessages = async (req, res) => {
  const groupCode = String(req.params.groupCode || '').toUpperCase();
  const afterId = Math.max(parseInt(req.query.afterId, 10) || 0, 0);
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 50, 1), 200);
  try {
    if (!(await canAccess(req.user, groupCode))) {
      return res.status(403).json({ success: false, message: 'You are not a member of this group' });
    }
    // Newest `limit` rows after afterId, returned oldest-first.
    const [rows] = await pool.query(
      `SELECT * FROM (${SELECT_MESSAGE} WHERE group_code = ? AND id > ? ORDER BY id DESC LIMIT ?) recent ORDER BY id`,
      [groupCode, afterId, limit]
    );
    res.json({ success: true, data: rows.map(toApi) });
  } catch (err) {
    console.error('getMessages failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error fetching messages' });
  }
};

// POST /api/chat/:groupCode/messages  { text }
exports.sendMessage = async (req, res) => {
  const groupCode = String(req.params.groupCode || '').toUpperCase();
  const text = typeof req.body.text === 'string' ? req.body.text.trim() : '';
  if (!text) return res.status(400).json({ success: false, message: 'Message text is required' });
  if (text.length > MAX_LENGTH) {
    return res.status(400).json({ success: false, message: `Message must be at most ${MAX_LENGTH} characters` });
  }
  try {
    if (!(await canAccess(req.user, groupCode))) {
      return res.status(403).json({ success: false, message: 'You are not a member of this group' });
    }
    // The sender's name comes from the account, never from the request body.
    const [[user]] = await pool.query('SELECT full_name FROM users WHERE id = ?', [req.user.id]);
    if (!user) return res.status(401).json({ success: false, message: 'Account no longer exists' });

    const [result] = await pool.query(
      'INSERT INTO chat_messages (group_code, sender_account_id, sender_name, message_text) VALUES (?, ?, ?, ?)',
      [groupCode, req.user.id, user.full_name, text]
    );
    const [[row]] = await pool.query(`${SELECT_MESSAGE} WHERE id = ?`, [result.insertId]);
    res.status(201).json({ success: true, message: toApi(row) });
  } catch (err) {
    console.error('sendMessage failed', err.code || err.message);
    res.status(500).json({ success: false, message: 'Server error sending message' });
  }
};
