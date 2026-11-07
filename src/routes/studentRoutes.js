const express = require('express');
const router = express.Router();
const studentsController = require('../controllers/studentsController');
const { requireAuth, requireRole } = require('../middleware/auth');
const { ROLES } = require('../constants');

const LECTURER = requireRole(ROLES.LECTURER);

// Roster: lecturers only. Students never see the full list.
router.get('/', requireAuth, LECTURER, studentsController.listStudents);
router.post('/', requireAuth, LECTURER, studentsController.createStudent);

// Single record: lecturers see any record; students see and edit only their own
// (ownership is checked in the controller, because it needs the database).
router.get('/:id', requireAuth, studentsController.getStudent);
router.put('/:id', requireAuth, studentsController.editStudent);

// Group placement and deletion: lecturers only.
router.post('/:id/assign', requireAuth, LECTURER, studentsController.assignToGroup);
router.post('/:id/unassign', requireAuth, LECTURER, studentsController.unassignFromGroup);
router.delete('/:id', requireAuth, LECTURER, studentsController.deleteStudent);

module.exports = router;
