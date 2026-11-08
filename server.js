require('dotenv').config(); // load .env before anything reads process.env

const express = require('express');
const cors = require('cors');
const pool = require('./src/db/pool');
const { sendError } = require('./src/util/http');

const app = express();
app.disable('x-powered-by');
// Behind a proxy (e.g. nginx or a host load balancer) set TRUST_PROXY=1 so per-IP limits see real clients.
app.set('trust proxy', process.env.TRUST_PROXY === '1' ? 1 : false);
app.use(cors());
app.use(express.json({ limit: '100kb' }));

app.use('/api/groups', require('./src/routes/groupRoutes'));
app.use('/api/auth', require('./src/routes/authRoutes'));
app.use('/api/students', require('./src/routes/studentRoutes'));
app.use('/api/operations', require('./src/routes/operationsRoutes'));
app.use('/api/requests', require('./src/routes/requestRoutes'));

// Health check: confirms the server can reach the database.
app.get('/api/health', async (req, res) => {
  const [[row]] = await pool.query('SELECT COUNT(*) AS total_groups FROM lab_groups');
  res.json({ status: 'OK', message: 'Server and database connected', groupsCount: row.total_groups });
});

app.use((req, res) => {
  sendError(res, 404, 'NOT_FOUND', `No route for ${req.method} ${req.path}`);
});

// Express 5 forwards rejected async handlers here, so database errors become JSON 500s.
// Internal details are logged, never sent to the client.
app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  if (err.type === 'entity.parse.failed') return sendError(res, 400, 'INVALID_JSON', 'Request body is not valid JSON');
  if (err.type === 'entity.too.large') return sendError(res, 413, 'PAYLOAD_TOO_LARGE', 'Request body is too large');
  console.error('Unhandled error:', err.code || err.message);
  return sendError(res, 500, 'SERVER_ERROR', 'Server error');
});

// Only start listening when run directly (npm start), so tests can import the app.
if (require.main === module) {
  const PORT = process.env.PORT || 5000;
  app.listen(PORT, () => console.log(`Server listening on port ${PORT}`));
}

module.exports = app;
