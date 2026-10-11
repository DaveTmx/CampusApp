// New file -- no student routes exist yet. Mount at app.use('/api/students', studentRoutes) in server.js.
const express = require('express');
const router = express.Router();
const studentsController = require('../controllers/studentsController');
const { requireAuth, requireRole } = require('../middleware/auth');

router.post('/:id/assign', requireAuth, requireRole('lecturer'), studentsController.assignToGroup);
router.put('/:id', requireAuth, studentsController.editStudent); // role check: student may only edit own record -- add ownership check once account_id wiring is in place
router.delete('/:id', requireAuth, requireRole('lecturer'), studentsController.deleteStudent);

module.exports = router;
