const express = require('express');
const router = express.Router();
const groupController = require('../contollers/groups.controller');
const { requireAuth, requireRole } = require('../middleware/auth');

// GET /api/groups                lab groups G01-G04 with member counts
router.get('/', requireAuth, groupController.getAllGroups);

// GET /api/groups/study          study groups (separate from the lab groups)
router.get('/study', requireAuth, groupController.getStudyGroups);

// POST /api/groups               create a study group
router.post('/', requireAuth, requireRole('lecturer'), groupController.createGroup);

// GET /api/groups/:code/members  lecturer: any group, student: own group only
router.get('/:code/members', requireAuth, groupController.getGroupMembers);

module.exports = router;
