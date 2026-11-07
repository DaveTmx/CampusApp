const express = require('express');
const router = express.Router();
const groupController = require('../controllers/groupController');
const { requireAuth, requireRole } = require('../middleware/auth');
const { ROLES } = require('../constants');

// GET /api/groups
router.get('/', requireAuth, groupController.getAllGroups);

// POST /api/groups (lecturers only)
router.post('/', requireAuth, requireRole(ROLES.LECTURER), groupController.createGroup);

module.exports = router;
