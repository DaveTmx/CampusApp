const express = require('express');
const router = express.Router();
const lecturersController = require('../contollers/lecturers.controllers');
const { requireAuth, requireRole } = require('../middleware/auth');

router.get('/', requireAuth, lecturersController.listLecturers);

router.get('/summary', requireAuth, requireRole('lecturer'), lecturersController.getGroupSummary);
router.get('/me', requireAuth, requireRole('lecturer'), lecturersController.getMyProfile);
router.put('/me', requireAuth, requireRole('lecturer'), lecturersController.updateMyProfile);

module.exports = router;
