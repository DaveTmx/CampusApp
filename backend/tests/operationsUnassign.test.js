// Run: npx jest --runInBand backend/tests/operationsUnassign.test.js
const request = require('supertest');
const path = require('path');
const crypto = require('crypto');
const pool = require(path.join(__dirname, '../../src/db/pool'));

const express = require('express');
const cors = require('cors');
const operationsRoutes = require(path.join(__dirname, '../../src/routes/operationsRoutes'));
const app = express();
app.use(cors());
app.use(express.json());
app.use('/api/operations', operationsRoutes);

const { makeLecturerToken } = require('./helpers/lecturerToken');
let auth;
beforeAll(async () => {
  auth = { Authorization: `Bearer ${await makeLecturerToken()}` };
});

describe('UNASSIGN_GROUP operation', () => {
  afterAll(async () => { await pool.end(); });

  beforeEach(async () => {
    await pool.query('DELETE FROM students');
    await pool.query(
      `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
       VALUES ('ops-unassign', 'Queued Student', '900000020', 'IT', 'G04', 1)`
    );
  });

  test('applies once and replays the same operationId', async () => {
    const operationId = crypto.randomUUID();
    const body = { operationId, opType: 'UNASSIGN_GROUP', payload: { studentId: 'ops-unassign' } };

    const first = await request(app).post('/api/operations').set(auth).send(body);
    expect(first.status).toBe(200);
    expect(first.body.replayed).toBe(false);

    const again = await request(app).post('/api/operations').set(auth).send(body);
    expect(again.status).toBe(200);
    expect(again.body.replayed).toBe(true);

    const [[row]] = await pool.query('SELECT group_code, version FROM students WHERE student_id = ?', ['ops-unassign']);
    expect(row.group_code).toBeNull();
    expect(row.version).toBe(2); // applied once, not twice
  });

  test('unknown student is refused with 404 and stored as a receipt', async () => {
    const res = await request(app).post('/api/operations').set(auth).send({
      operationId: crypto.randomUUID(), opType: 'UNASSIGN_GROUP', payload: { studentId: 'no-such-student' },
    });
    expect(res.status).toBe(404);
    expect(res.body.status).toBe('FAILED');
  });
});
