const express = require('express');
const router = express.Router();
const operationsController = require('../controllers/operationsController');
const { requireAuth, requireRole } = require('../middleware/auth');
const { ROLES } = require('../constants');

// POST /api/operations (lecturers only; every opType changes lecturer-controlled data)
router.post('/', requireAuth, requireRole(ROLES.LECTURER), operationsController.submitOperation);

module.exports = router;
