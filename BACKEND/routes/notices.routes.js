const express = require('express');
const router = express.Router();
const noticeController = require('../contollers/notice.controller');
const { requireAuth, requireRole } = require('../middleware/auth');

router.get('/', requireAuth, noticeController.listNotices);
router.post('/', requireAuth, requireRole('lecturer'), noticeController.createNotice);

router.post('/read-all', requireAuth, noticeController.markAllRead);
router.post('/:id/read', requireAuth, noticeController.markRead);
router.delete('/:id', requireAuth, requireRole('lecturer'), noticeController.deleteNotice);

module.exports = router;
