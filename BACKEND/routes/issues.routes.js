const express = require('express');
const router = express.Router();
const issuesController = require('../contollers/issues.controller');
const { requireAuth, requireRole } = require('../middleware/auth');

// POST /api/issues            report an issue
router.post('/', requireAuth, issuesController.createIssue);

// GET /api/issues/mine        the caller's own reports
router.get('/mine', requireAuth, issuesController.listMyIssues);

// GET /api/issues?status=     all reports (lecturer)
router.get('/', requireAuth, requireRole('lecturer'), issuesController.listAllIssues);

// PATCH /api/issues/:id/status   { status }
router.patch('/:id/status', requireAuth, requireRole('lecturer'), issuesController.updateIssueStatus);

module.exports = router;
