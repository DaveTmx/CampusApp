require('dotenv').config(); // load .env before anything reads process.env

const express = require('express');
const cors = require('cors');
const pool = require('./src/db/pool');

const app = express();

app.use(cors());
app.use(express.json());

// Import Routes
const authRoutes = require('./src/routes/authRoutes');
const groupRoutes = require('./src/routes/groupRoutes');
// const studentRoutes = require('./src/routes/studentRoutes'); // Commented out until created
// const operationsRoutes = require('./src/routes/operationsRoutes'); // Commented out until created

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/groups', groupRoutes);
// app.use('/api/students', studentRoutes); // Commented out until created
// app.use('/api/operations', operationsRoutes); // Commented out until created

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