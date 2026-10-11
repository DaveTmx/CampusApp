const express = require('express');
const router = express.Router();
const { register, login } = require('../controllers/authController');
const { protect } = require('../middleware/authMiddleware');

// Public routes
router.post('/register', register);
router.post('/login', login);

// Protected route example
router.get('/me', protect, (req, res) => {
  res.status(200).json({
    success: true,
    message: 'Access granted to protected route',
    user: req.user
  });
});

module.exports = router;