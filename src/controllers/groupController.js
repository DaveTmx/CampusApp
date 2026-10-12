const db = require('../db/pool');

// @desc    Create a new study group
// @route   POST /api/groups
// @access  Private
const createGroup = async (req, res) => {
  try {
    const { name, course_code, description, max_members } = req.body;
    const creator_id = req.user.id;

    if (!name || !course_code) {
      return res.status(400).json({
        success: false,
        message: 'Group name and course code are required'
      });
    }

    const [result] = await db.execute(
      'INSERT INTO study_groups (name, course_code, description, max_members, created_by) VALUES (?, ?, ?, ?, ?)',
      [name, course_code, description || '', max_members || 5, creator_id]
    );

    await db.execute(
      'INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)',
      [result.insertId, creator_id, 'leader']
    );

    res.status(201).json({
      success: true,
      message: 'Study group created successfully',
      groupId: result.insertId
    });
  } catch (error) {
    console.error('Create group error:', error);
    res.status(500).json({ success: false, message: 'Server error creating group' });
  }
};

// @desc    Get all study groups
// @route   GET /api/groups
// @access  Private
const getGroups = async (req, res) => {
  try {
    const [groups] = await db.execute(
      `SELECT g.*, u.full_name AS creator_name, COUNT(m.user_id) AS member_count 
       FROM study_groups g 
       LEFT JOIN users u ON g.created_by = u.id 
       LEFT JOIN group_members m ON g.id = m.group_id 
       GROUP BY g.id`
    );

    res.status(200).json({
      success: true,
      data: groups
    });
  } catch (error) {
    console.error('Get groups error:', error);
    res.status(500).json({ success: false, message: 'Server error fetching groups' });
  }
};

// @desc    Get single group details with member list
// @route   GET /api/groups/:id
// @access  Private
const getGroupById = async (req, res) => {
  try {
    const groupId = req.params.id;

    const [groups] = await db.execute(
      `SELECT g.*, u.full_name AS creator_name 
       FROM study_groups g 
       LEFT JOIN users u ON g.created_by = u.id 
       WHERE g.id = ?`,
      [groupId]
    );

    if (groups.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Study group not found'
      });
    }

    const [members] = await db.execute(
      `SELECT m.user_id, m.role, m.joined_at, u.full_name, u.email 
       FROM group_members m 
       JOIN users u ON m.user_id = u.id 
       WHERE m.group_id = ?`,
      [groupId]
    );

    res.status(200).json({
      success: true,
      data: {
        ...groups[0],
        members
      }
    });
  } catch (error) {
    console.error('Get group details error:', error);
    res.status(500).json({ success: false, message: 'Server error fetching group details' });
  }
};

// @desc    Join a study group
// @route   POST /api/groups/:id/join
// @access  Private
const joinGroup = async (req, res) => {
  try {
    const groupId = req.params.id;
    const userId = req.user.id;

    const [existing] = await db.execute(
      'SELECT * FROM group_members WHERE group_id = ? AND user_id = ?',
      [groupId, userId]
    );

    if (existing.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'You are already a member of this group'
      });
    }

    await db.execute(
      'INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)',
      [groupId, userId, 'member']
    );

    res.status(200).json({
      success: true,
      message: 'Successfully joined the group'
    });
  } catch (error) {
    console.error('Join group error:', error);
    res.status(500).json({ success: false, message: 'Server error joining group' });
  }
};

// @desc    Leave a study group
// @route   POST /api/groups/:id/leave
// @access  Private
const leaveGroup = async (req, res) => {
  try {
    const groupId = req.params.id;
    const userId = req.user.id;

    const [existing] = await db.execute(
      'SELECT * FROM group_members WHERE group_id = ? AND user_id = ?',
      [groupId, userId]
    );

    if (existing.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'You are not a member of this group'
      });
    }

    if (existing[0].role === 'leader') {
      return res.status(400).json({
        success: false,
        message: 'Group leaders cannot leave. Delete the group or transfer leadership first.'
      });
    }

    await db.execute(
      'DELETE FROM group_members WHERE group_id = ? AND user_id = ?',
      [groupId, userId]
    );

    res.status(200).json({
      success: true,
      message: 'Successfully left the group'
    });
  } catch (error) {
    console.error('Leave group error:', error);
    res.status(500).json({ success: false, message: 'Server error leaving group' });
  }
};

module.exports = {
  createGroup,
  getGroups,
  getGroupById,
  joinGroup,
  leaveGroup
};