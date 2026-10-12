// Everything that changes data "later" or "by approval":
//  - POST /api/sync                  idempotent operation (retry-safe, operationId)
//  - /api/sync/pending...            student change requests the lecturer approves
const express = require('express');
const router = express.Router();
const studentsController = require('../contollers/students.controller');
const { executeOperation } = require('../services/operations.service');
const { requireAuth, requireRole } = require('../middleware/auth');

// POST /api/sync  { operationId, opType: 'ASSIGN_GROUP' | 'DELETE_STUDENT', payload }
// Send the same operationId again after a dropped response and you get the
// original result back with replayed: true; nothing is applied twice.
router.post('/', requireAuth, requireRole('lecturer'), async (req, res) => {
  const { operationId, opType, payload } = req.body;
  const { httpStatus, body } = await executeOperation({
    operationId,
    opType,
    payload,
    accountId: req.user ? req.user.id : null,
  });
  res.status(httpStatus).json(body);
});

// Change-request queue (the lecturer's "Sync status" screen)
router.get('/pending', requireAuth, requireRole('lecturer'), studentsController.listPendingChanges);
router.post('/pending', requireAuth, requireRole('student'), studentsController.createPendingChange);
router.post('/pending/:id/approve', requireAuth, requireRole('lecturer'), studentsController.approvePendingChange);
router.post('/pending/:id/reject', requireAuth, requireRole('lecturer'), studentsController.rejectPendingChange);
router.post('/approve-all', requireAuth, requireRole('lecturer'), studentsController.approveAllPendingChanges);

module.exports = router;
