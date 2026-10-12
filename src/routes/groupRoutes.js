const express = require('express');
const router = express.Router();
const { 
  createGroup, 
  getGroups, 
  getGroupById, 
  joinGroup, 
  leaveGroup 
} = require('../controllers/groupController');
const { protect } = require('../middleware/authMiddleware');

router.use(protect);

router.post('/', createGroup);
router.get('/', getGroups);
router.get('/:id', getGroupById);
router.post('/:id/join', joinGroup);
router.post('/:id/leave', leaveGroup);

module.exports = router;