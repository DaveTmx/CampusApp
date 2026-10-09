const pool = require('../db/pool');

// Get all study groups
exports.getAllGroups = async (req, res) => {
  try {
   const [rows] = await pool.query('SELECT * FROM lab_groups'); // Adjust table name if different
    res.status(200).json({ success: true, data: rows });
  } catch (error) {
    console.error('Error fetching groups:', error);
    res.status(500).json({ success: false, message: 'Server error fetching groups' });
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
    const [result] = await pool.query(query, [name, description, course_code]);

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