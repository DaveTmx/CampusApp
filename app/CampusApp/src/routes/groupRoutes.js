const express = require('express');
const router = express.Router();
const groupController = require('../controllers/groupController');

// GET /api/groups
router.get('/', groupController.getAllGroups);

// POST /api/groups
router.post('/', groupController.createGroup);

module.exports = router;