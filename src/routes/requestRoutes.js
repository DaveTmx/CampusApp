const express = require('express');
const router = express.Router();
const requestsController = require('../controllers/requestsController');
const { requireAuth, requireRole } = require('../middleware/auth');
const { ROLES } = require('../constants');

const LECTURER = requireRole(ROLES.LECTURER);

// Lecturer decisions on student requests.
router.get('/', requireAuth, LECTURER, requestsController.listRequests);
router.post('/:id/approve', requireAuth, LECTURER, requestsController.approve);
router.post('/:id/reject', requireAuth, LECTURER, requestsController.reject);

module.exports = router;
