require('dotenv').config(); // load .env before anything reads process.env

const express = require('express');
const cors = require('cors');
const pool = require('./config/db');

const app = express();

app.use(cors());
app.use(express.json());

// Import Routes
const authRoutes = require('./routes/auth.routes');
const studentRoutes = require('./routes/students.routes');
const groupRoutes = require('./routes/groups.routes');
const chatRoutes = require('./routes/chat.routes');
const noticeRoutes = require('./routes/notices.routes');
const issueRoutes = require('./routes/issues.routes');
const scheduleRoutes = require('./routes/schedules.routes');
const lecturerRoutes = require('./routes/lecturers.routes');
const syncRoutes = require('./routes/sync.routes');

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/students', studentRoutes);
app.use('/api/groups', groupRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/notices', noticeRoutes);
app.use('/api/issues', issueRoutes);
app.use('/api/schedules', scheduleRoutes);
app.use('/api/lecturers', lecturerRoutes);
app.use('/api/sync', syncRoutes);

// Health check endpoint
app.get('/api/health', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT COUNT(*) AS total_groups FROM lab_groups');
    res.json({
      status: 'OK',
      message: 'Server and Database connected successfully!',
      groupsCount: rows[0].total_groups
    });
  } catch (err) {
    res.status(500).json({ status: 'Database Error', message: err.message });
  }
});

// Unknown route
app.use((req, res) => {
  res.status(404).json({ success: false, message: `Route not found: ${req.method} ${req.originalUrl}` });
});

// Last-resort error handler (bad JSON bodies, thrown errors)
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  const status = err.status || err.statusCode || 500;
  if (status >= 500) console.error('Unhandled error:', err);
  res.status(status).json({ success: false, message: status >= 500 ? 'Server error' : err.message });
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
