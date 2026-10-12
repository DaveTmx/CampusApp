const express = require('express');
const router = express.Router();
const studentsController = require('../contollers/students.controller');
const { requireAuth, requireRole } = require('../middleware/auth');

// GET /api/students?q=&page=&pageSize=   (the lecturer roster)
router.get('/', requireAuth, requireRole('lecturer'), studentsController.listStudents);
// POST /api/students
router.post('/', requireAuth, requireRole('lecturer'), studentsController.createStudent);

// "/me" must be declared before "/:id" or it would be read as an id.
router.get('/me', requireAuth, studentsController.getMyStudent);

router.get('/:id', requireAuth, studentsController.getStudent); // lecturer, or the owner
router.post('/:id/assign', requireAuth, requireRole('lecturer'), studentsController.assignToGroup);
router.put('/:id', requireAuth, studentsController.editStudent); // a student may only edit their own record (checked in the controller)
router.delete('/:id', requireAuth, requireRole('lecturer'), studentsController.deleteStudent);

module.exports = router;
