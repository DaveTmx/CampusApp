// New file. Mount at app.use('/api/operations', operationsRoutes) in server.js.
const express = require('express');
const router = express.Router();
const operationsController = require('../controllers/operationsController');
const { requireAuth } = require('../middleware/auth');

router.post('/', requireAuth, operationsController.submitOperation);

module.exports = router;
