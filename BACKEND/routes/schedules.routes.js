const express = require('express');
const router = express.Router();
const schedulesController = require('../contollers/schedules.controller');
const { requireAuth, requireRole } = require('../middleware/auth');

// GET /api/schedules?day=0..4
router.get('/', requireAuth, schedulesController.getSchedule);

router.post('/', requireAuth, requireRole('lecturer'), schedulesController.createSession);
router.put('/:id', requireAuth, requireRole('lecturer'), schedulesController.updateSession);
router.delete('/:id', requireAuth, requireRole('lecturer'), schedulesController.deleteSession);

module.exports = router;
