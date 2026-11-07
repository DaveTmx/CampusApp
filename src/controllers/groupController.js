'use strict';

const pool = require('../db/pool');
const { sendError } = require('../util/http');
const { cleanText } = require('../util/validation');

// GET /api/groups   (any signed-in user)
// Returns each lab group with how many active students it holds.
exports.getAllGroups = async (req, res) => {
  const [rows] = await pool.query(
    `SELECT g.group_code, g.capacity, COUNT(s.student_id) AS occupied
       FROM lab_groups g
       LEFT JOIN students s ON s.group_code = g.group_code AND s.deleted_at IS NULL
      GROUP BY g.group_code, g.capacity
      ORDER BY g.group_code`
  );
  res.json({ success: true, data: rows });
};

// POST /api/groups  { name, description?, courseCode? }   (lecturer)
// Creates a campus study group in study_groups. This is not a lab group.
exports.createGroup = async (req, res) => {
  const name = cleanText(req.body?.name, 100);
  if (!name) return sendError(res, 400, 'VALIDATION_FAILED', 'Group name is required (max 100 characters)');
  const description = cleanText(req.body?.description, 500);
  const courseCode = cleanText(req.body?.courseCode, 20);

  const [result] = await pool.query(
    'INSERT INTO study_groups (name, description, course_code, created_by) VALUES (?, ?, ?, ?)',
    [name, description, courseCode, req.user.id]
  );
  res.status(201).json({ success: true, groupId: result.insertId });
};
