const express = require('express');
const cors = require('cors');
const pool = require('./src/db/pool');

const app = express();

app.use(cors());
app.use(express.json());

// Import Routes
const groupRoutes = require('./src/routes/groupRoutes');
const authRoutes = require('./src/routes/authRoutes'); // <-- Check this line
const studentRoutes = require('./src/routes/studentRoutes');
const operationsRoutes = require('./src/routes/operationsRoutes');

// Mount Routes
app.use('/api/groups', groupRoutes);
app.use('/api/auth', authRoutes); // <-- Check this line
app.use('/api/students', studentRoutes);
app.use('/api/operations', operationsRoutes);

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

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});