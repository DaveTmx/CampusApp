const pool = require('../config/db');

// Get all lab groups (G01-G04) with how many students each one holds
exports.getAllGroups = async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT g.group_code, g.capacity, COUNT(s.student_id) AS member_count
         FROM lab_groups g
         LEFT JOIN students s ON s.group_code = g.group_code AND s.deleted_at IS NULL
        GROUP BY g.group_code, g.capacity
        ORDER BY g.group_code`
    );
    res.status(200).json({ success: true, data: rows });
  } catch (error) {
    console.error('Error fetching groups:', error);
    res.status(500).json({ success: false, message: 'Server error fetching groups' });
  }
};

// Members of one lab group. A lecturer can see any group; a student only their own.
exports.getGroupMembers = async (req, res) => {
  const code = String(req.params.code || '').toUpperCase();
  try {
    if (req.user.role !== 'lecturer') {
      const [own] = await pool.query(
        'SELECT group_code FROM students WHERE account_id = ? AND deleted_at IS NULL', [req.user.id]
      );
      if (own.length === 0 || own[0].group_code !== code) {
        return res.status(403).json({ success: false, message: 'You can only view your own group' });
      }
    }
    const [group] = await pool.query('SELECT group_code, capacity FROM lab_groups WHERE group_code = ?', [code]);
    if (group.length === 0) return res.status(404).json({ success: false, error: 'NO_SUCH_GROUP' });

    const [members] = await pool.query(
      `SELECT student_id, student_number, student_name AS full_name, programme_code AS programme
         FROM students WHERE group_code = ? AND deleted_at IS NULL ORDER BY student_name`,
      [code]
    );
    res.status(200).json({ success: true, group: group[0], data: members });
  } catch (error) {
    console.error('Error fetching group members:', error);
    res.status(500).json({ success: false, message: 'Server error fetching members' });
  }
};

// Get all study groups (the separate "campus life" feature)
exports.getStudyGroups = async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM study_groups ORDER BY created_at DESC');
    res.status(200).json({ success: true, data: rows });
  } catch (error) {
    console.error('Error fetching study groups:', error);
    res.status(500).json({ success: false, message: 'Server error fetching study groups' });
  }
};

// Create a new study group
exports.createGroup = async (req, res) => {
  const { name, description, course_code } = req.body;

  if (!name) {
    return res.status(400).json({ success: false, message: 'Group name is required' });
  }

  try {
    const query = 'INSERT INTO study_groups (name, description, course_code) VALUES (?, ?, ?)';
    const [result] = await pool.query(query, [name, description ?? null, course_code ?? null]);

    res.status(201).json({
      success: true,
      message: 'Group created successfully',
      groupId: result.insertId
    });
  } catch (error) {
    console.error('Error creating group:', error);
    res.status(500).json({ success: false, message: 'Server error creating group' });
  }
};
