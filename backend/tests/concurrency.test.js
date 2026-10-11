// Challenge 1. Run: npx jest backend/tests/concurrency.test.js
// Requires: a real MySQL `ict361_lab` test database with database/schema.sql
// applied, and the server NOT separately running (supertest drives the
// Express app in-process). Needs devDependencies: jest, supertest.
const request = require('supertest');
const path = require('path');
const fs = require('fs');
const pool = require(path.join(__dirname, '../../src/db/pool'));

// Build the app the same way server.js does, without calling .listen().
const express = require('express');
const cors = require('cors');
const studentRoutes = require(path.join(__dirname, '../../src/routes/studentRoutes'));
const app = express();
app.use(cors());
app.use(express.json());
app.use('/api/students', studentRoutes);

const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require(path.join(__dirname, '../../src/middleware/auth'));
const lecturerToken = jwt.sign({ id: 1, email: 'lecturer@test', role: 'lecturer' }, JWT_SECRET);

async function resetFixture() {
  const sql = fs.readFileSync(path.join(__dirname, '../../database/seed-test-fixture.sql'), 'utf8');
  const conn = await pool.getConnection();
  try {
    for (const stmt of sql.split(';').map((s) => s.trim()).filter(Boolean)) {
      await conn.query(stmt);
    }
  } finally {
    conn.release();
  }
}

async function countG01() {
  const [[{ cnt }]] = await pool.query(
    "SELECT COUNT(*) AS cnt FROM students WHERE group_code = 'G01' AND deleted_at IS NULL"
  );
  return cnt;
}

describe('Challenge 1: atomic 15-student group concurrency limit', () => {
  afterAll(async () => { await pool.end(); });

  test('20 repetitions: exactly one of two simultaneous requests wins the last place', async () => {
    for (let i = 1; i <= 20; i++) {
      await resetFixture();

      const [resA, resB] = await Promise.all([
        request(app).post('/api/students/contender-a-0000-0000-0000-000000000001/assign')
          .set('Authorization', `Bearer ${lecturerToken}`).send({ groupCode: 'G01' }),
        request(app).post('/api/students/contender-b-0000-0000-0000-000000000002/assign')
          .set('Authorization', `Bearer ${lecturerToken}`).send({ groupCode: 'G01' }),
      ]);

      const succeeded = [resA, resB].filter((r) => r.status === 200).length;
      const full = [resA, resB].filter((r) => r.status === 409 && r.body.error === 'GROUP_FULL').length;
      const finalCount = await countG01();

      console.log(`Run ${i}: succeeded=${succeeded} full=${full} finalCount=${finalCount}`);
      expect(succeeded).toBe(1);
      expect(full).toBe(1);
      expect(finalCount).toBe(15);
    }
  });
});
