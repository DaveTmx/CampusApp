const express = require('express');
const router = express.Router();
const chatController = require('../contollers/chat.controller');
const { requireAuth } = require('../middleware/auth');

// GET  /api/chat/:groupCode/messages?afterId=&limit=
router.get('/:groupCode/messages', requireAuth, chatController.getMessages);

// POST /api/chat/:groupCode/messages   { text }
router.post('/:groupCode/messages', requireAuth, chatController.sendMessage);

module.exports = router;
